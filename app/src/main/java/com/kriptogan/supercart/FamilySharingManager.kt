package com.kriptogan.supercart

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.atomic.AtomicInteger
import com.kriptogan.supercart.FirebaseFunctionsHelper

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
    
    // NEW: Enhanced timestamp tracking with more granular control
    private var lastLocalActionTimestamps: MutableMap<String, Long> = mutableMapOf()
    private var lastLocalUpdateTimestamps: MutableMap<String, Long> = mutableMapOf()
    private var pendingLocalChanges: MutableSet<String> = mutableSetOf()
    
    // NEW: Minimum time window to protect local changes (reduced to 15s for better propagation)
    private val localActionProtectionWindowMs = 15000L // 15 seconds
    
    // NEW: Track the last Firebase update timestamp to detect stale updates
    private var lastFirebaseUpdateTime = 0L
    
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
    
    // Enhanced update family data with better retry mechanism and improved local change tracking
    fun updateFamilyData(groceries: List<GroceryWithDate>, categories: List<CustomCategory>, boughtItems: List<GroceryWithDate> = emptyList()) {
        if (!isSharingEnabled || currentProjectId == null) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        
        // Track when local update happens and what the data was
        lastLocalUpdateTime = currentTime
        lastLocalGroceries = groceries
        lastLocalCategories = categories
        
        // NEW: Enhanced tracking of local changes with more granular control
        groceries.forEach { grocery ->
            val itemKey = "${grocery.name}_${grocery.customCategoryId}"
            lastLocalActionTimestamps[itemKey] = currentTime
            lastLocalUpdateTimestamps[itemKey] = currentTime
            pendingLocalChanges.add(itemKey)
        }
        
        println("DEBUG: FamilySharingManager.updateFamilyData called at $currentTime")
        println("DEBUG: - groceries: ${groceries.size}, categories: ${categories.size}")
        println("DEBUG: - protected items: ${pendingLocalChanges.size}")
        println("DEBUG: - bought items count: ${groceries.count { it.isBought }}")
        println("DEBUG: - time since last local update: ${if (lastLocalUpdateTime > 0) currentTime - lastLocalUpdateTime else "N/A"}ms")
        
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
                    timestamp = currentTime,
                    retryCount = 0
                )
                pendingUpdates.add(update)
                pendingUpdatesCount = pendingUpdates.size
                
                println("DEBUG: Sending update to Firebase - projectId: $currentProjectId")
                println("DEBUG: - bought items being sent: ${groceries.count { it.isBought }}")
                val success = firebaseService.updateFamilyProject(currentProjectId!!, groceries, categories)
                
                if (success) {
                    lastSyncTime = System.currentTimeMillis()
                    retryCount.set(0)
                    pendingUpdates.removeAll { it.id == update.id }
                    pendingUpdatesCount = pendingUpdates.size
                    syncProgress = 1f
                    onSyncStatusChange?.invoke(false, null)
                    println("DEBUG: Firebase update successful - sent ${groceries.count { it.isBought }} bought items")
                    clearPendingLocalChanges() // Clear pending changes after successful sync
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
    
    // Start real-time sync with enhanced error handling and stale update protection
    fun startRealTimeSync(projectId: String) {
        syncListener?.remove()
        syncListener = firebaseService.listenToFamilyProject(projectId) { familyProject ->
            try {
                familyProject?.let { project ->
                    val currentTime = System.currentTimeMillis()
                    lastFirebaseUpdateTime = currentTime
                    
                    val firebaseGroceries = project.groceries.map { it.withLocalDate() }
                    val firebaseCategories = project.categories
                    
                    // NEW: Clear protections for items that have been successfully synced
                    clearProtectionsForSyncedItems(firebaseGroceries)
                    
                    // NEW: Enhanced prioritization with stale update detection
                    val prioritizedGroceries = prioritizeLocalActions(firebaseGroceries)
                    val prioritizedCategories = firebaseCategories // Categories don't have per-item conflicts
                    
                    println("DEBUG: Applying Firebase update at $currentTime with enhanced local action protection")
                    println("DEBUG: Local update time: $lastLocalUpdateTime, Protection window: ${localActionProtectionWindowMs}ms")
                    println("DEBUG: - received ${firebaseGroceries.count { it.isBought }} bought items from Firebase")
                    println("DEBUG: - applying ${prioritizedGroceries.count { it.isBought }} bought items after prioritization")
                    
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
    
    // NEW: Enhanced prioritize local actions with smarter protection logic
    private fun prioritizeLocalActions(firebaseGroceries: List<GroceryWithDate>): List<GroceryWithDate> {
        // Clean up old timestamps first
        cleanupOldTimestamps()
        
        val currentTime = System.currentTimeMillis()
        val prioritizedGroceries = mutableListOf<GroceryWithDate>()
        
        // NEW: More intelligent stale update detection - only protect if we have recent local changes
        val hasRecentLocalChanges = lastLocalUpdateTime > 0 && 
            (currentTime - lastLocalUpdateTime) < localActionProtectionWindowMs &&
            pendingLocalChanges.isNotEmpty()
        
        if (hasRecentLocalChanges) {
            println("DEBUG: Detected recent local changes - applying selective protection")
        } else {
            println("DEBUG: No recent local changes - allowing Firebase updates to propagate")
        }
        
        firebaseGroceries.forEach { firebaseGrocery ->
            val itemKey = "${firebaseGrocery.name}_${firebaseGrocery.customCategoryId}"
            val localActionTime = lastLocalActionTimestamps[itemKey] ?: 0L
            val localUpdateTime = lastLocalUpdateTimestamps[itemKey] ?: 0L
            val isPendingLocalChange = pendingLocalChanges.contains(itemKey)
            
            // NEW: Enhanced protection logic - only protect if we have recent local changes
            val hasRecentLocalAction = localActionTime > 0 && 
                (currentTime - localActionTime) < localActionProtectionWindowMs
            
            val hasRecentLocalUpdate = localUpdateTime > 0 && 
                (currentTime - localUpdateTime) < localActionProtectionWindowMs
            
            val shouldProtectLocal = hasRecentLocalChanges && (hasRecentLocalAction || hasRecentLocalUpdate || isPendingLocalChange)
            
            if (shouldProtectLocal) {
                // Find the corresponding local item
                val localItem = lastLocalGroceries.find { localGrocery ->
                    localGrocery.name == firebaseGrocery.name && 
                    localGrocery.customCategoryId == firebaseGrocery.customCategoryId
                }
                
                if (localItem != null) {
                    val timeSinceLocalAction = currentTime - localActionTime
                    println("DEBUG: Protecting local action for '${firebaseGrocery.name}' (local action was ${timeSinceLocalAction}ms ago)")
                    prioritizedGroceries.add(localItem)
                } else {
                    // Local item not found, use Firebase item
                    prioritizedGroceries.add(firebaseGrocery)
                }
            } else {
                // No recent local action or no recent local changes, use Firebase item
                prioritizedGroceries.add(firebaseGrocery)
            }
        }
        
        // Add any local items that don't exist in Firebase and are still protected
        lastLocalGroceries.forEach { localGrocery ->
            val itemKey = "${localGrocery.name}_${localGrocery.customCategoryId}"
            val localActionTime = lastLocalActionTimestamps[itemKey] ?: 0L
            val localUpdateTime = lastLocalUpdateTimestamps[itemKey] ?: 0L
            val isPendingLocalChange = pendingLocalChanges.contains(itemKey)
            
            val hasRecentLocalAction = localActionTime > 0 && 
                (currentTime - localActionTime) < localActionProtectionWindowMs
            
            val hasRecentLocalUpdate = localUpdateTime > 0 && 
                (currentTime - localUpdateTime) < localActionProtectionWindowMs
            
            val shouldProtectLocal = hasRecentLocalChanges && (hasRecentLocalAction || hasRecentLocalUpdate || isPendingLocalChange)
            
            val existsInFirebase = firebaseGroceries.any { firebaseGrocery ->
                firebaseGrocery.name == localGrocery.name && 
                firebaseGrocery.customCategoryId == localGrocery.customCategoryId
            }
            
            if (!existsInFirebase && shouldProtectLocal) {
                println("DEBUG: Adding protected local-only item '${localGrocery.name}' to prioritized list")
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
        lastFirebaseUpdateTime = 0L
        lastLocalGroceries = emptyList()
        lastLocalCategories = emptyList()
        lastLocalActionTimestamps.clear()
        lastLocalUpdateTimestamps.clear()
        pendingLocalChanges.clear()
        println("DEBUG: Force cleared all local tracking data")
    }
    
    // NEW: Clean up old timestamps to prevent memory leaks
    private fun cleanupOldTimestamps() {
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - 60000 // Remove timestamps older than 1 minute
        
        // Clean up action timestamps
        val actionKeysToRemove = lastLocalActionTimestamps.entries
            .filter { it.value < cutoffTime }
            .map { it.key }
        
        actionKeysToRemove.forEach { key ->
            lastLocalActionTimestamps.remove(key)
        }
        
        // Clean up update timestamps
        val updateKeysToRemove = lastLocalUpdateTimestamps.entries
            .filter { it.value < cutoffTime }
            .map { it.key }
        
        updateKeysToRemove.forEach { key ->
            lastLocalUpdateTimestamps.remove(key)
        }
        
        // Clean up pending changes that are no longer recent
        val pendingKeysToRemove = pendingLocalChanges.filter { key ->
            val actionTime = lastLocalActionTimestamps[key] ?: 0L
            val updateTime = lastLocalUpdateTimestamps[key] ?: 0L
            val mostRecentTime = maxOf(actionTime, updateTime)
            mostRecentTime < cutoffTime
        }
        
        pendingKeysToRemove.forEach { key ->
            pendingLocalChanges.remove(key)
        }
        
        if (actionKeysToRemove.isNotEmpty() || updateKeysToRemove.isNotEmpty() || pendingKeysToRemove.isNotEmpty()) {
            println("DEBUG: Cleaned up ${actionKeysToRemove.size} action timestamps, ${updateKeysToRemove.size} update timestamps, ${pendingKeysToRemove.size} pending changes")
        }
    }
    
    // NEW: Enhanced clear pending local changes after successful sync
    private fun clearPendingLocalChanges() {
        // Only clear protections for items that have been successfully synced
        // This allows new changes to be sent while protecting recent ones
        val currentTime = System.currentTimeMillis()
        val keysToRemove = mutableListOf<String>()
        
        pendingLocalChanges.forEach { itemKey ->
            // Check if this item has been synced (older than 2 seconds)
            val itemUpdateTime = lastLocalUpdateTimestamps[itemKey] ?: 0L
            if (itemUpdateTime > 0 && (currentTime - itemUpdateTime) > 2000) {
                keysToRemove.add(itemKey)
                println("DEBUG: Clearing protection for synced item: $itemKey")
            }
        }
        
        keysToRemove.forEach { key ->
            pendingLocalChanges.remove(key)
            lastLocalActionTimestamps.remove(key)
            lastLocalUpdateTimestamps.remove(key)
        }
        
        if (keysToRemove.isNotEmpty()) {
            println("DEBUG: Cleared protections for ${keysToRemove.size} synced items")
            // Force sync to other devices after clearing protections
            forceSyncToOtherDevices()
        } else {
            println("DEBUG: Keeping protections active for recent changes")
        }
    }
    
    // NEW: Enhanced clear pending local changes after successful sync
    private fun clearProtectionsForSyncedItems(firebaseGroceries: List<GroceryWithDate>) {
        val currentTime = System.currentTimeMillis()
        val keysToRemove = mutableListOf<String>()
        
        pendingLocalChanges.forEach { itemKey ->
            // Check if this item exists in Firebase and has been synced
            val firebaseItem = firebaseGroceries.find { firebaseGrocery ->
                "${firebaseGrocery.name}_${firebaseGrocery.customCategoryId}" == itemKey
            }
            
            if (firebaseItem != null) {
                // Item exists in Firebase, check if our local changes have been applied
                val localItem = lastLocalGroceries.find { localGrocery ->
                    "${localGrocery.name}_${localGrocery.customCategoryId}" == itemKey
                }
                
                if (localItem != null) {
                    // If Firebase item matches our local item, clear protection
                    if (localItem.inShoppingList == firebaseItem.inShoppingList && 
                        localItem.isBought == firebaseItem.isBought) {
                        keysToRemove.add(itemKey)
                        println("DEBUG: Clearing protection for '${localItem.name}' - changes synced to Firebase")
                    }
                }
            }
        }
        
        keysToRemove.forEach { key ->
            pendingLocalChanges.remove(key)
            lastLocalActionTimestamps.remove(key)
            lastLocalUpdateTimestamps.remove(key)
        }
        
        if (keysToRemove.isNotEmpty()) {
            println("DEBUG: Cleared protections for ${keysToRemove.size} synced items")
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
                    
                    // Check for expired items after successful sync
                    checkExpiredItemsAfterSync()
                } catch (e: Exception) {
                    println("Error in force sync update: ${e.message}")
                }
            }
        }
    }
    
    // Check for expired items after sync operations
    private suspend fun checkExpiredItemsAfterSync() {
        if (isSharingEnabled && currentProjectId != null) {
            try {
                FirebaseFunctionsHelper.checkExpiredItems(
                    currentProjectId!!,
                    onSuccess = { result ->
                        val expiredCount = result["expiredItems"] ?: 0
                        val expiringCount = result["expiringItems"] ?: 0
                        val dueCount = result["dueItems"] ?: 0
                        val notificationsSent = result["notificationsSent"] ?: 0
                        
                        println("After sync - Expired items: $expiredCount, Expiring soon: $expiringCount, Due for purchase: $dueCount, Notifications sent: $notificationsSent")
                    },
                    onFailure = { error ->
                        println("Failed to check expired items after sync: $error")
                    }
                )
            } catch (e: Exception) {
                println("Error checking expired items after sync: ${e.message}")
            }
        }
    }
    
    // NEW: Force sync changes to other devices
    fun forceSyncToOtherDevices() {
        if (isSharingEnabled && currentProjectId != null) {
            scope.launch {
                try {
                    println("DEBUG: Force syncing changes to other devices")
                    // Send current local data to Firebase to ensure other devices receive it
                    firebaseService.updateFamilyProject(currentProjectId!!, lastLocalGroceries, lastLocalCategories)
                } catch (e: Exception) {
                    println("Error in force sync to other devices: ${e.message}")
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
    
    // NEW: Enhanced debug function to help track sync issues
    fun getSyncDebugInfo(): String {
        val currentTime = System.currentTimeMillis()
        val protectedItemsCount = pendingLocalChanges.size
        val actionTimestampsCount = lastLocalActionTimestamps.size
        val updateTimestampsCount = lastLocalUpdateTimestamps.size
        
        val hasRecentLocalChanges = lastLocalUpdateTime > 0 && 
            (currentTime - lastLocalUpdateTime) < localActionProtectionWindowMs &&
            pendingLocalChanges.isNotEmpty()
        
        return """
            Sync Debug Info:
            - Connection: $connectionStatus
            - Syncing: $isSyncing
            - Last local update: ${if (lastLocalUpdateTime > 0) "${currentTime - lastLocalUpdateTime}ms ago" else "Never"}
            - Last Firebase update: ${if (lastFirebaseUpdateTime > 0) "${currentTime - lastFirebaseUpdateTime}ms ago" else "Never"}
            - Protected items: $protectedItemsCount
            - Action timestamps: $actionTimestampsCount
            - Update timestamps: $updateTimestampsCount
            - Protection window: ${localActionProtectionWindowMs}ms
            - Pending updates: $pendingUpdatesCount
            - Has recent local changes: $hasRecentLocalChanges
            - Protection active: ${if (hasRecentLocalChanges) "YES" else "NO"}
        """.trimIndent()
    }
    
    // NEW: Public method to check if there are recent local changes
    fun hasRecentLocalChanges(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastLocalUpdateTime
        
        // Only consider changes "recent" if they're very recent (within 10 seconds)
        // and we have pending changes that need protection
        val isVeryRecent = timeSinceLastUpdate < 10000 // 10 seconds to give more time for propagation
        val hasPendingChanges = pendingLocalChanges.isNotEmpty()
        
        val result = lastLocalUpdateTime > 0 && isVeryRecent && hasPendingChanges
        
        if (result) {
            println("DEBUG: Has recent local changes - time since last update: ${timeSinceLastUpdate}ms, pending changes: ${pendingLocalChanges.size}")
        }
        
        return result
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