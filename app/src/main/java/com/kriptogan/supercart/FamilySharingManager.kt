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
    
    // Callbacks
    var onDataUpdate: ((List<GroceryWithDate>, List<CustomCategory>) -> Unit)? = null
    var onBoughtItemsUpdate: ((List<GroceryWithDate>) -> Unit)? = null
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
                    boughtItems = boughtItems,
                    timestamp = System.currentTimeMillis(),
                    retryCount = 0
                )
                pendingUpdates.add(update)
                pendingUpdatesCount = pendingUpdates.size
                
                val success = firebaseService.updateFamilyProject(currentProjectId!!, groceries, categories, boughtItems)
                
                if (success) {
                    lastSyncTime = System.currentTimeMillis()
                    retryCount.set(0)
                    pendingUpdates.removeAll { it.id == update.id }
                    pendingUpdatesCount = pendingUpdates.size
                    syncProgress = 1f
                    onSyncStatusChange?.invoke(false, null)
                } else {
                    syncError = "Failed to sync changes"
                    onSyncStatusChange?.invoke(false, syncError)
                    
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
                    update.categories,
                    update.boughtItems
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
                    val groceries = project.groceries.map { it.withLocalDate() }
                    val categories = project.categories
                    val boughtItems = project.boughtItems.map { it.withLocalDate() }
                    
                    onDataUpdate?.invoke(groceries, categories)
                    onBoughtItemsUpdate?.invoke(boughtItems)
                    
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
    val boughtItems: List<GroceryWithDate>,
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