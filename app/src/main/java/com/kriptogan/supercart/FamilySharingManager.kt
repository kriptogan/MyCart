package com.kriptogan.supercart

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.atomic.AtomicInteger

class FamilySharingManager(
    private val firebaseService: FirebaseService,
    private val scope: CoroutineScope
) {
    // UI State
    var isSharingEnabled by mutableStateOf(false)
    var currentProjectId by mutableStateOf<String?>(null)
    var familyCode by mutableStateOf<String?>(null)
    var joinCode by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    
    // Enhanced sync status
    var isSyncing by mutableStateOf(false)
    var lastSyncTime by mutableStateOf<Long?>(null)
    var syncError by mutableStateOf<String?>(null)
    var syncProgress by mutableStateOf(0f) // 0.0 to 1.0
    var pendingUpdatesCount by mutableStateOf(0)
    var connectionStatus by mutableStateOf<ConnectionStatus>(ConnectionStatus.UNKNOWN)
    
    // Real-time sync
    private var syncListener: ListenerRegistration? = null
    
    // Enhanced retry mechanism
    private var pendingUpdates = mutableListOf<PendingUpdate>()
    private var retryCount = AtomicInteger(0)
    private val maxRetries = 5
    private val retryDelayMs = 2000L
    
    // NEW: Debouncing mechanism to prevent immediate overwrites
    private var lastLocalUpdateTime = 0L
    private val debounceDelayMs = 500L // Reduced from 2000ms to 500ms for better responsiveness
    
    // NEW: Track the last local data to compare with Firebase updates
    private var lastLocalGroceries: List<GroceryWithDate> = emptyList()
    private var lastLocalCategories: List<CustomCategory> = emptyList()
    
    // NEW: Track the timestamp of the last local action for each item
    private var lastLocalActionTimestamps: MutableMap<String, Long> = mutableMapOf()
    
    // Callbacks
    var onDataUpdate: ((List<GroceryWithDate>, List<CustomCategory>) -> Unit)? = null
    var onSyncStatusChange: ((Boolean, String?) -> Unit)? = null
    var onConnectionStatusChange: ((ConnectionStatus) -> Unit)? = null
    
    // Create new family sharing
    fun createFamily(groceries: List<GroceryWithDate>, categories: List<CustomCategory>) {
        scope.launch {
            isLoading = true
            errorMessage = null
            connectionStatus = ConnectionStatus.CONNECTING
            
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
                    connectionStatus = ConnectionStatus.CONNECTED
                    
                    // Clear debounce to allow immediate initial sync
                    clearDebounce()
                    forceClearLocalTracking() // Force clear local tracking on create
                    
                    startRealTimeSync(projectId)
                } else {
                    errorMessage = "Failed to create family sharing"
                    connectionStatus = ConnectionStatus.FAILED
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                connectionStatus = ConnectionStatus.FAILED
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
            connectionStatus = ConnectionStatus.CONNECTING
            
            try {
                val deviceId = firebaseService.getDeviceId()
                val familyProject = firebaseService.joinFamilyProject(code, deviceId)
                
                if (familyProject != null) {
                    currentProjectId = code
                    familyCode = code
                    isSharingEnabled = true
                    connectionStatus = ConnectionStatus.CONNECTED
                    
                    // Clear debounce to allow immediate initial sync
                    clearDebounce()
                    forceClearLocalTracking() // Force clear local tracking on join
                    
                    // Update local data with family data
                    val groceries = familyProject.groceries.map { it.withLocalDate() }
                    val categories = familyProject.categories
                    
                    onDataUpdate?.invoke(groceries, categories)
                    
                    startRealTimeSync(code)
                } else {
                    errorMessage = "Invalid family code or already a member"
                    connectionStatus = ConnectionStatus.FAILED
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                connectionStatus = ConnectionStatus.FAILED
            } finally {
                isLoading = false
            }
        }
    }
    
    // Enhanced update family data with better retry mechanism
    fun updateFamilyData(groceries: List<GroceryWithDate>, categories: List<CustomCategory>, boughtItems: List<GroceryWithDate> = emptyList()) {
        if (!isSharingEnabled || currentProjectId == null) {
            return
        }
        
        // Track when local update happens and what the data was
        lastLocalUpdateTime = System.currentTimeMillis()
        lastLocalGroceries = groceries
        lastLocalCategories = categories
        
        // NEW: Track timestamps for each item's last local action (including bought items)
        val currentTime = System.currentTimeMillis()
        groceries.forEach { grocery ->
            val itemKey = "${grocery.name}_${grocery.customCategoryId}"
            lastLocalActionTimestamps[itemKey] = currentTime
        }
        

        
        println("DEBUG: Local update triggered - groceries: ${groceries.size}, categories: ${categories.size}")
        
        scope.launch {
            isSyncing = true
            syncError = null
            syncProgress = 0f
            onSyncStatusChange?.invoke(true, null)
            
            try {
                // Add to pending updates for tracking
                val update = PendingUpdate(
                    id = System.currentTimeMillis(),
                    groceries = groceries,
                    categories = categories,
                    timestamp = System.currentTimeMillis(),
                    retryCount = 0
                )
                pendingUpdates.add(update)
                pendingUpdatesCount = pendingUpdates.size
                
                println("DEBUG: Sending update to Firebase - projectId: $currentProjectId")
                val success = firebaseService.updateFamilyProject(currentProjectId!!, groceries, categories)
                
                if (success) {
                    lastSyncTime = System.currentTimeMillis()
                    retryCount.set(0)
                    pendingUpdates.removeAll { it.id == update.id }
                    pendingUpdatesCount = pendingUpdates.size
                    syncProgress = 1f
                    onSyncStatusChange?.invoke(false, null)
                    println("DEBUG: Firebase update successful")
                } else {
                    syncError = "Failed to sync changes"
                    onSyncStatusChange?.invoke(false, syncError)
                    println("DEBUG: Firebase update failed")
                    
                    // Enhanced retry logic
                    if (update.retryCount < maxRetries) {
                        update.retryCount++
                        delay(retryDelayMs * update.retryCount) // Exponential backoff
                        retryPendingUpdates()
                    }
                }
            } catch (e: Exception) {
                syncError = "Sync error: ${e.message}"
                onSyncStatusChange?.invoke(false, syncError)
                println("Error syncing family data: ${e.message}")
            } finally {
                isSyncing = false
                syncProgress = 0f
            }
        }
    }
    
    // Enhanced retry pending updates
    private suspend fun retryPendingUpdates() {
        if (pendingUpdates.isEmpty()) {
            return
        }
        
        val updatesToRetry = pendingUpdates.toList()
        
        for (update in updatesToRetry) {
            try {
                                    val success = firebaseService.updateFamilyProject(
                        currentProjectId!!, 
                        update.groceries, 
                        update.categories
                    )
                
                if (success) {
                    lastSyncTime = System.currentTimeMillis()
                    pendingUpdates.remove(update)
                    pendingUpdatesCount = pendingUpdates.size
                } else {
                    if (update.retryCount >= maxRetries) {
                        // Remove failed updates after max retries
                        pendingUpdates.remove(update)
                        pendingUpdatesCount = pendingUpdates.size
                    }
                }
            } catch (e: Exception) {
                println("Retry failed for update ${update.id}: ${e.message}")
                if (update.retryCount >= maxRetries) {
                    pendingUpdates.remove(update)
                    pendingUpdatesCount = pendingUpdates.size
                }
            }
        }
        
        // If we still have pending updates, retry again
        if (pendingUpdates.isNotEmpty()) {
            delay(retryDelayMs)
            retryPendingUpdates()
        }
    }
    
    // Start real-time sync with enhanced error handling
    fun startRealTimeSync(projectId: String) {
        syncListener?.remove()
        syncListener = firebaseService.listenToFamilyProject(projectId) { familyProject ->
            try {
                familyProject?.let { project ->
                    val firebaseGroceries = project.groceries.map { it.withLocalDate() }
                    val firebaseCategories = project.categories
                    
                    // NEW: Prioritize local actions based on timestamps
                    val prioritizedGroceries = prioritizeLocalActions(firebaseGroceries)
                    val prioritizedCategories = firebaseCategories // Categories don't have per-item conflicts
                    
                    println("DEBUG: Applying Firebase update with local action prioritization")
                    
                    onDataUpdate?.invoke(prioritizedGroceries, prioritizedCategories)
                    
                    // Update connection status
                    connectionStatus = ConnectionStatus.CONNECTED
                    onConnectionStatusChange?.invoke(ConnectionStatus.CONNECTED)
                } ?: run {
                    connectionStatus = ConnectionStatus.DISCONNECTED
                    onConnectionStatusChange?.invoke(ConnectionStatus.DISCONNECTED)
                }
            } catch (e: Exception) {
                connectionStatus = ConnectionStatus.ERROR
                onConnectionStatusChange?.invoke(ConnectionStatus.ERROR)
                println("Error in real-time sync: ${e.message}")
            }
        }
    }
    
    // NEW: Prioritize local actions over Firebase updates based on timestamps
    private fun prioritizeLocalActions(firebaseGroceries: List<GroceryWithDate>): List<GroceryWithDate> {
        // Clean up old timestamps first
        cleanupOldTimestamps()
        
        val currentTime = System.currentTimeMillis()
        val prioritizedGroceries = mutableListOf<GroceryWithDate>()
        
        firebaseGroceries.forEach { firebaseGrocery ->
            val itemKey = "${firebaseGrocery.name}_${firebaseGrocery.customCategoryId}"
            val localActionTime = lastLocalActionTimestamps[itemKey] ?: 0L
            
            // Check if we have a more recent local action for this item
            val hasRecentLocalAction = localActionTime > 0 && 
                (currentTime - localActionTime) < 10000 // 10 seconds window
            
            if (hasRecentLocalAction) {
                // Find the corresponding local item
                val localItem = lastLocalGroceries.find { localGrocery ->
                    localGrocery.name == firebaseGrocery.name && 
                    localGrocery.customCategoryId == firebaseGrocery.customCategoryId
                }
                
                if (localItem != null) {
                    println("DEBUG: Prioritizing local action for '${firebaseGrocery.name}' (local action was ${currentTime - localActionTime}ms ago)")
                    prioritizedGroceries.add(localItem)
                } else {
                    // Local item not found, use Firebase item
                    prioritizedGroceries.add(firebaseGrocery)
                }
            } else {
                // No recent local action, use Firebase item
                prioritizedGroceries.add(firebaseGrocery)
            }
        }
        
        // Add any local items that don't exist in Firebase
        lastLocalGroceries.forEach { localGrocery ->
            val itemKey = "${localGrocery.name}_${localGrocery.customCategoryId}"
            val localActionTime = lastLocalActionTimestamps[itemKey] ?: 0L
            val hasRecentLocalAction = localActionTime > 0 && 
                (currentTime - localActionTime) < 10000 // 10 seconds window
            
            val existsInFirebase = firebaseGroceries.any { firebaseGrocery ->
                firebaseGrocery.name == localGrocery.name && 
                firebaseGrocery.customCategoryId == localGrocery.customCategoryId
            }
            
            if (!existsInFirebase && hasRecentLocalAction) {
                println("DEBUG: Adding local-only item '${localGrocery.name}' to prioritized list")
                prioritizedGroceries.add(localGrocery)
            }
        }
        
        return prioritizedGroceries
    }
    

    
    // Stop real-time sync
    fun stopSync() {
        syncListener?.remove()
        syncListener = null
        connectionStatus = ConnectionStatus.DISCONNECTED
        onConnectionStatusChange?.invoke(ConnectionStatus.DISCONNECTED)
    }
    
    // Leave family with cleanup
    fun leaveFamily() {
        stopSync()
        currentProjectId = null
        familyCode = null
        isSharingEnabled = false
        connectionStatus = ConnectionStatus.DISCONNECTED
        pendingUpdates.clear()
        pendingUpdatesCount = 0
        retryCount.set(0)
        // Note: Local data remains unchanged - only disconnecting from family sharing
    }
    
    // Process offline queue when connection is restored
    fun processOfflineQueue() {
        if (!isSharingEnabled || currentProjectId == null) {
            return
        }
        
        scope.launch {
            try {
                connectionStatus = ConnectionStatus.CONNECTING
                firebaseService.processOfflineQueue()
                connectionStatus = ConnectionStatus.CONNECTED
            } catch (e: Exception) {
                connectionStatus = ConnectionStatus.ERROR
                println("Error processing offline queue: ${e.message}")
            }
        }
    }
    
    // Clear error
    fun clearError() {
        errorMessage = null
        syncError = null
    }
    
    // NEW: Clear debounce timer (useful when joining family or forcing update)
    fun clearDebounce() {
        lastLocalUpdateTime = 0L
    }
    
    // NEW: Force clear all local tracking (useful when joining family)
    fun forceClearLocalTracking() {
        lastLocalUpdateTime = 0L
        lastLocalGroceries = emptyList()
        lastLocalCategories = emptyList()
        lastLocalActionTimestamps.clear()
    }
    
    // NEW: Clean up old timestamps to prevent memory leaks
    private fun cleanupOldTimestamps() {
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - 60000 // Remove timestamps older than 1 minute
        
        val keysToRemove = lastLocalActionTimestamps.entries
            .filter { it.value < cutoffTime }
            .map { it.key }
        
        keysToRemove.forEach { key ->
            lastLocalActionTimestamps.remove(key)
        }
        
        if (keysToRemove.isNotEmpty()) {
            println("DEBUG: Cleaned up ${keysToRemove.size} old timestamps")
        }
    }
    
    // NEW: Force sync update (bypasses debounce)
    fun forceSyncUpdate() {
        clearDebounce()
        // Trigger an immediate sync if we have pending data
        if (isSharingEnabled && currentProjectId != null) {
            // Launch a coroutine to handle the suspend function call
            scope.launch {
                try {
                    // This will trigger the real-time listener immediately
                    firebaseService.updateFamilyProject(currentProjectId!!, emptyList(), emptyList())
                } catch (e: Exception) {
                    println("Error in force sync update: ${e.message}")
                }
            }
        }
    }
    
    // Enhanced join code validation
    fun validateJoinCode(code: String): Boolean {
        return code.length == 8 && code.all { it.isDigit() } && code != "00000000"
    }
    
    // Get sync status summary
    fun getSyncStatusSummary(): String {
        return when {
            isSyncing -> "Syncing..."
            syncError != null -> "Sync Error"
            pendingUpdatesCount > 0 -> "$pendingUpdatesCount pending updates"
            lastSyncTime != null -> "Last synced: ${formatLastSyncTime(lastSyncTime!!)}"
            else -> "Not synced"
        }
    }
    
    private fun formatLastSyncTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }
}

// Enhanced data classes
data class PendingUpdate(
    val id: Long,
    val groceries: List<GroceryWithDate>,
    val categories: List<CustomCategory>,
    val timestamp: Long,
    var retryCount: Int
)

enum class ConnectionStatus {
    UNKNOWN,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR,
    FAILED
} 