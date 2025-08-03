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
    
    // Real-time sync
    private var syncListener: ListenerRegistration? = null
    
         // Callbacks
     var onDataUpdate: ((List<GroceryWithDate>, List<CustomCategory>) -> Unit)? = null
     var onDialogClose: (() -> Unit)? = null
    
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
    
    // Update family data
    fun updateFamilyData(groceries: List<GroceryWithDate>, categories: List<CustomCategory>) {
        currentProjectId?.let { projectId ->
            scope.launch {
                try {
                    firebaseService.updateFamilyProject(projectId, groceries, categories)
                } catch (e: Exception) {
                    // Handle update error
                }
            }
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
    
    // Clear error
    fun clearError() {
        errorMessage = null
    }
    
    // Validate join code
    fun validateJoinCode(code: String): Boolean {
        return code.length == 8 && code.all { it.isDigit() }
    }
} 