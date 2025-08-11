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
                    // Handle groceries without IDs from Firebase (backward compatibility)
                    val groceriesWithIds = if (familyProject.groceries.hasMissingIds()) {
                        println("DEBUG: Found groceries without IDs in Firebase, assigning IDs...")
                        familyProject.groceries.assignMissingIds()
                    } else {
                        familyProject.groceries
                    }
                    
                    val groceries = groceriesWithIds.map { it.withLocalDate() }
                    val categories = familyProject.categories
                    
                    onDataUpdate?.invoke(groceries, categories)
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
    
    // Note: updateFamilyData removed - only create/join operations sync to cloud
    
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
    
    // Note: Real-time sync removed - data only syncs during create/join operations
    
    // Note: Priority logic removed since real-time sync is disabled
    

    
    // Stop sync (simplified since no real-time sync)
    fun stopSync() {
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
    
    // Note: Complex sync protection logic removed since automatic sync is disabled
    
    // Note: Force sync functions removed since automatic sync is disabled
    
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