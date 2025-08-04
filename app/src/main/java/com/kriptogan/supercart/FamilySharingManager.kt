package com.kriptogan.supercart

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.google.firebase.firestore.ListenerRegistration

class FamilySharingManager(
    private val firebaseService: FirebaseService,
    private val scope: CoroutineScope
) {
    // UI State
    var isSharingEnabled by mutableStateOf(false)
    var currentProjectId by mutableStateOf<String?>(null)
    var familyCode by mutableStateOf<String?>(null)
    var showCreateFamilyDialog by mutableStateOf(false)
    var showJoinFamilyDialog by mutableStateOf(false)
    var showFamilyCodeDialog by mutableStateOf(false)
    var joinCode by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    
    // Sync status
    var isSyncing by mutableStateOf(false)
    var lastSyncTime by mutableStateOf<Long?>(null)
    var syncError by mutableStateOf<String?>(null)
    
    // Real-time sync
    private var syncListener: ListenerRegistration? = null
    
    // Retry mechanism
    private var pendingUpdates = mutableListOf<Pair<List<GroceryWithDate>, List<CustomCategory>>>()
    private var retryCount = 0
    private val maxRetries = 3
    
    // Callbacks
    var onDataUpdate: ((List<GroceryWithDate>, List<CustomCategory>) -> Unit)? = null
    var onDialogClose: (() -> Unit)? = null
    var onSyncStatusChange: ((Boolean, String?) -> Unit)? = null
    
    // Create new family sharing
    fun createFamily(groceries: List<GroceryWithDate>, categories: List<CustomCategory>) {
        scope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val deviceId = firebaseService.getDeviceId()
                val projectId = firebaseService.generateFamilyCode()
                
                val success = firebaseService.createFamilyProject(
                    projectId = projectId,
                    groceries = groceries,
                    categories = categories,
                    deviceId = deviceId
                )
                
                                                  if (success) {
                     currentProjectId = projectId
                     familyCode = projectId
                     isSharingEnabled = true
                     onDialogClose?.invoke()  // Notify MainActivity to close dialog
                     showFamilyCodeDialog = true
                     startRealTimeSync(projectId)
                 } else {
                     errorMessage = "Failed to create family sharing"
                     onDialogClose?.invoke()  // Notify MainActivity to close dialog
                 }
             } catch (e: Exception) {
                 errorMessage = "Error: ${e.message}"
                 onDialogClose?.invoke()  // Notify MainActivity to close dialog
             } finally {
                 isLoading = false
             }
        }
    }
    
    // Join existing family
    fun joinFamily(code: String) {
        scope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                val deviceId = firebaseService.getDeviceId()
                val familyProject = firebaseService.joinFamilyProject(code, deviceId)
                
                                                  if (familyProject != null) {
                     currentProjectId = code
                     familyCode = code
                     isSharingEnabled = true
                     onDialogClose?.invoke()  // Notify MainActivity to close dialog
                     
                     // Update local data with family data
                     val groceries = familyProject.groceries.map { it.withLocalDate() }
                     val categories = familyProject.categories
                     
                     onDataUpdate?.invoke(groceries, categories)
                     
                     startRealTimeSync(code)
                 } else {
                     errorMessage = "Invalid family code or already a member"
                     onDialogClose?.invoke()  // Notify MainActivity to close dialog
                 }
             } catch (e: Exception) {
                 errorMessage = "Error: ${e.message}"
                 onDialogClose?.invoke()  // Notify MainActivity to close dialog
             } finally {
                 isLoading = false
             }
        }
    }
    
    // Update family data with retry mechanism
    fun updateFamilyData(groceries: List<GroceryWithDate>, categories: List<CustomCategory>) {
        if (!isSharingEnabled || currentProjectId == null) {
            return
        }
        
        scope.launch {
            isSyncing = true
            syncError = null
            onSyncStatusChange?.invoke(true, null)
            
            try {
                val success = firebaseService.updateFamilyProject(currentProjectId!!, groceries, categories)
                
                if (success) {
                    lastSyncTime = System.currentTimeMillis()
                    retryCount = 0
                    // Clear any pending updates that were successfully synced
                    pendingUpdates.clear()
                    onSyncStatusChange?.invoke(false, null)
                } else {
                    // Add to pending updates for retry
                    pendingUpdates.add(Pair(groceries, categories))
                    syncError = "Failed to sync changes"
                    onSyncStatusChange?.invoke(false, syncError)
                    
                    // Retry after a delay
                    if (retryCount < maxRetries) {
                        retryCount++
                        kotlinx.coroutines.delay(2000L * retryCount) // Exponential backoff
                        retryPendingUpdates()
                    }
                }
            } catch (e: Exception) {
                syncError = "Sync error: ${e.message}"
                onSyncStatusChange?.invoke(false, syncError)
                println("Error syncing family data: ${e.message}")
            } finally {
                isSyncing = false
            }
        }
    }
    
    // Retry pending updates
    private suspend fun retryPendingUpdates() {
        if (pendingUpdates.isEmpty() || retryCount >= maxRetries) {
            return
        }
        
        val updates = pendingUpdates.toList()
        pendingUpdates.clear()
        
        for ((groceries, categories) in updates) {
            try {
                val success = firebaseService.updateFamilyProject(currentProjectId!!, groceries, categories)
                if (success) {
                    lastSyncTime = System.currentTimeMillis()
                    retryCount = 0
                } else {
                    // Add back to pending updates
                    pendingUpdates.add(Pair(groceries, categories))
                }
            } catch (e: Exception) {
                pendingUpdates.add(Pair(groceries, categories))
                println("Retry failed: ${e.message}")
            }
        }
        
        // If we still have pending updates, retry again
        if (pendingUpdates.isNotEmpty() && retryCount < maxRetries) {
            retryCount++
            kotlinx.coroutines.delay(2000L * retryCount)
            retryPendingUpdates()
        }
    }
    
    // Start real-time sync
    fun startRealTimeSync(projectId: String) {
        syncListener?.remove()
        syncListener = firebaseService.listenToFamilyProject(projectId) { familyProject ->
            familyProject?.let { project ->
                val groceries = project.groceries.map { it.withLocalDate() }
                val categories = project.categories
                onDataUpdate?.invoke(groceries, categories)
            }
        }
    }
    
    // Stop real-time sync
    fun stopSync() {
        syncListener?.remove()
        syncListener = null
    }
    
    // Leave family
    fun leaveFamily() {
        stopSync()
        currentProjectId = null
        familyCode = null
        isSharingEnabled = false
        // Note: Local data remains unchanged - only disconnecting from family sharing
    }
    
    // Process offline queue when connection is restored
    fun processOfflineQueue() {
        if (!isSharingEnabled || currentProjectId == null) {
            return
        }
        
        scope.launch {
            try {
                firebaseService.processOfflineQueue()
            } catch (e: Exception) {
                println("Error processing offline queue: ${e.message}")
            }
        }
    }
    
    // Clear error
    fun clearError() {
        errorMessage = null
        syncError = null
    }
    
    // Validate join code
    fun validateJoinCode(code: String): Boolean {
        return code.length == 8 && code.all { it.isDigit() }
    }
} 