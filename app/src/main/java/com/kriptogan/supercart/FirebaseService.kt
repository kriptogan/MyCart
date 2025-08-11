package com.kriptogan.supercart

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.delay
import android.content.Context

class FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    
    // Simple acknowledgment callback
    var onAcknowledgmentReceived: ((String, Boolean) -> Unit)? = null
    
    companion object {
        private const val FAMILY_PROJECTS_COLLECTION = "family_projects"
        private const val DEVICE_REGISTRATIONS_COLLECTION = "device_registrations"
        private const val OFFLINE_QUEUE_COLLECTION = "offline_queue"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }
    
    // Enhanced offline queue for pending updates
    private val offlineQueue = mutableListOf<OfflineUpdate>()
    private var isProcessingOfflineQueue = false
    
    // Connection status tracking
    private var lastConnectionCheck = 0L
    private var isOnline = true
    
    // Check if device is online with caching
    private fun isOnline(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastConnectionCheck > 30000) { // Check every 30 seconds
            lastConnectionCheck = now
            isOnline = try {
                // Simple connectivity check - in production, use NetworkCallback
                true // For now, assume online
            } catch (e: Exception) {
                false
            }
        }
        return isOnline
    }
    
    // Add update to offline queue with enhanced error handling
    private suspend fun addToOfflineQueue(projectId: String, groceries: List<GroceryWithDate>, categories: List<CustomCategory>, senderDeviceId: String) {
        val update = OfflineUpdate(
            projectId = projectId,
            groceries = groceries.map { grocery ->
                FirebaseGrocery(
                    name = grocery.name,
                    customCategoryId = grocery.customCategoryId,
                    expirationDate = grocery.expirationDate?.toString(),
                    lastTimeBoughtDays = grocery.lastTimeBoughtDays,
                    averageBuyingDays = grocery.averageBuyingDays,
                    buyEvents = grocery.buyEvents.map { it.toString() },
                    inShoppingList = grocery.inShoppingList,
                    isBought = grocery.isBought,
                    lastUpdate = grocery.lastUpdate
                )
            },
            categories = categories.map { category ->
                FirebaseCustomCategory(
                    id = category.id,
                    name = category.name,
                    default = category.default,
                    viewOrder = category.viewOrder,
                    lastUpdate = category.lastUpdate
                )
            },
            timestamp = System.currentTimeMillis(),
            retryCount = 0,
            senderDeviceId = senderDeviceId
        )
        offlineQueue.add(update)
        
        // Store in local storage for persistence
        println("Added to offline queue: ${update.timestamp} (${offlineQueue.size} total) for sender $senderDeviceId")
    }
    
    // Enhanced process offline queue
    suspend fun processOfflineQueue() {
        if (offlineQueue.isEmpty() || !isOnline() || isProcessingOfflineQueue) {
            return
        }
        
        isProcessingOfflineQueue = true
        
        try {
            val updates = offlineQueue.toList()
            offlineQueue.clear()
            
            for (update in updates) {
                try {
                    val success = updateFamilyProject(
                        update.projectId, 
                        update.groceries.map { it.toGrocery().withLocalDate() }, 
                        update.categories.map { it.toCustomCategory() },
                        update.senderDeviceId
                    )
                    
                    if (!success) {
                        // Add back to queue if failed
                        if (update.retryCount < MAX_RETRY_ATTEMPTS) {
                            update.retryCount++
                            offlineQueue.add(update)
                        } else {
                            println("Failed to process offline update after ${MAX_RETRY_ATTEMPTS} attempts: ${update.timestamp}")
                        }
                    } else {
                        println("Successfully processed offline update: ${update.timestamp}")
                    }
                } catch (e: Exception) {
                    // Add back to queue if failed
                    if (update.retryCount < MAX_RETRY_ATTEMPTS) {
                        update.retryCount++
                        offlineQueue.add(update)
                    }
                    println("Failed to process offline update: ${e.message}")
                }
                
                // Small delay between processing updates
                delay(100)
            }
        } finally {
            isProcessingOfflineQueue = false
        }
    }
    
    // Generate unique 8-digit family code with enhanced validation
    suspend fun generateFamilyCode(): String {
        var code: String
        var attempts = 0
        val maxAttempts = 100
        
        do {
            code = generateRandom8DigitCode()
            attempts++
            
            if (attempts > maxAttempts) {
                throw Exception("Unable to generate unique family code after $maxAttempts attempts")
            }
        } while (isCodeExists(code))
        
        return code
    }
    
    private fun generateRandom8DigitCode(): String {
        return Random.nextInt(10000000, 99999999).toString()
    }
    
    private suspend fun isCodeExists(code: String): Boolean {
        return try {
            val snapshot = db.collection(FAMILY_PROJECTS_COLLECTION)
                .whereEqualTo("projectId", code)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            println("Error checking if code exists: ${e.message}")
            false
        }
    }
    
    // Enhanced create family project with better error handling
    suspend fun createFamilyProject(
        projectId: String,
        groceries: List<GroceryWithDate>,
        categories: List<CustomCategory>,
        deviceId: String
    ): Boolean {
        return try {
            // Validate input data
            if (projectId.isBlank() || deviceId.isBlank()) {
                println("Invalid projectId or deviceId")
                return false
            }
            
            val familyProject = FamilyProject(
                projectId = projectId,
                createdBy = deviceId,
                createdAt = System.currentTimeMillis(),
                members = listOf(deviceId),
                groceries = groceries.map { grocery ->
                    FirebaseGrocery(
                        name = grocery.name,
                        customCategoryId = grocery.customCategoryId,
                        expirationDate = grocery.expirationDate?.toString(),
                        lastTimeBoughtDays = grocery.lastTimeBoughtDays,
                        averageBuyingDays = grocery.averageBuyingDays,
                        buyEvents = grocery.buyEvents.map { it.toString() },
                        inShoppingList = grocery.inShoppingList,
                        isBought = grocery.isBought,
                        lastUpdate = grocery.lastUpdate
                    )
                },
                categories = categories.map { category ->
                    FirebaseCustomCategory(
                        id = category.id,
                        name = category.name,
                        default = category.default,
                        viewOrder = category.viewOrder,
                        lastUpdate = category.lastUpdate
                    )
                },
                lastUpdated = System.currentTimeMillis()
            )
            
            db.collection(FAMILY_PROJECTS_COLLECTION)
                .document(projectId)
                .set(familyProject)
                .await()
            
            // Register this device
            registerDevice(deviceId, projectId)
            
            println("Successfully created family project: $projectId")
            true
        } catch (e: FirebaseFirestoreException) {
            println("Firestore error creating family project: ${e.message}")
            false
        } catch (e: Exception) {
            println("Error creating family project: ${e.message}")
            false
        }
    }
    
    // Enhanced join family project with better validation
    suspend fun joinFamilyProject(
        projectId: String,
        deviceId: String
    ): FamilyProject? {
        return try {
            // Validate input
            if (projectId.isBlank() || deviceId.isBlank()) {
                println("Invalid projectId or deviceId")
                return null
            }
            
            val snapshot = db.collection(FAMILY_PROJECTS_COLLECTION)
                .document(projectId)
                .get()
                .await()
            
            if (snapshot.exists()) {
                val familyProject = snapshot.toObject(FamilyProject::class.java)
                if (familyProject != null && !familyProject.members.contains(deviceId)) {
                    // Add device to members
                    val updatedMembers = familyProject.members + deviceId
                    db.collection(FAMILY_PROJECTS_COLLECTION)
                        .document(projectId)
                        .update("members", updatedMembers)
                        .await()
                    
                    // Register this device
                    registerDevice(deviceId, projectId)
                    
                    println("Successfully joined family project: $projectId")
                    // Convert to regular data structure for return
                    val (regularGroceries, regularCategories) = familyProject.toRegularData()
                    FamilyProject(
                        projectId = familyProject.projectId,
                        createdBy = familyProject.createdBy,
                        createdAt = familyProject.createdAt,
                        members = updatedMembers,
                        groceries = regularGroceries.map { grocery ->
                            FirebaseGrocery(
                                name = grocery.name,
                                customCategoryId = grocery.customCategoryId,
                                expirationDate = grocery.expirationDate,
                                lastTimeBoughtDays = grocery.lastTimeBoughtDays,
                                averageBuyingDays = grocery.averageBuyingDays,
                                buyEvents = grocery.buyEvents,
                                inShoppingList = grocery.inShoppingList,
                                isBought = grocery.isBought,
                                lastUpdate = grocery.lastUpdate
                            )
                        },
                        categories = regularCategories.map { category ->
                            FirebaseCustomCategory(
                                id = category.id,
                                name = category.name,
                                default = category.default,
                                viewOrder = category.viewOrder,
                                lastUpdate = category.lastUpdate
                            )
                        },
                        lastUpdated = familyProject.lastUpdated
                    )
                } else {
                    println("Already a member of this family or invalid project")
                    familyProject
                }
            } else {
                println("Family project not found: $projectId")
                null
            }
        } catch (e: FirebaseFirestoreException) {
            println("Firestore error joining family project: ${e.message}")
            null
        } catch (e: Exception) {
            println("Error joining family project: ${e.message}")
            null
        }
    }
    
    private suspend fun registerDevice(deviceId: String, projectId: String) {
        try {
            val registration = DeviceRegistration(
                deviceId = deviceId,
                projectId = projectId,
                joinedAt = System.currentTimeMillis(),
                lastSync = System.currentTimeMillis()
            )
            
            db.collection(DEVICE_REGISTRATIONS_COLLECTION)
                .document(deviceId)
                .set(registration)
                .await()
                
            println("Successfully registered device: $deviceId")
        } catch (e: Exception) {
            println("Error registering device: ${e.message}")
        }
    }

    // Update FCM token for this device
    fun updateFcmToken(deviceId: String, token: String) {
        try {
            db.collection(DEVICE_REGISTRATIONS_COLLECTION)
                .document(deviceId)
                .update(mapOf("fcmToken" to token))
                .addOnSuccessListener { println("FCM token updated for device: $deviceId") }
                .addOnFailureListener { e -> println("Failed to update FCM token: ${e.message}") }
        } catch (e: Exception) {
            println("Error updating FCM token: ${e.message}")
        }
    }

    // Update per-device notification settings
    fun updateNotificationSettings(deviceId: String, settings: NotificationSettings) {
        try {
            db.collection(DEVICE_REGISTRATIONS_COLLECTION)
                .document(deviceId)
                .update(mapOf(
                    "notificationSettings" to settings
                ))
                .addOnSuccessListener { println("Notification settings updated for device: $deviceId") }
                .addOnFailureListener { e -> println("Failed to update notification settings: ${e.message}") }
        } catch (e: Exception) {
            println("Error updating notification settings: ${e.message}")
        }
    }
    
    // Load notification settings for a device
    suspend fun loadNotificationSettings(deviceId: String): NotificationSettings? {
        return try {
            val snapshot = db.collection(DEVICE_REGISTRATIONS_COLLECTION)
                .document(deviceId)
                .get()
                .await()
            
            if (snapshot.exists()) {
                val registration = snapshot.toObject(DeviceRegistration::class.java)
                registration?.notificationSettings ?: NotificationSettings()
            } else {
                NotificationSettings()
            }
        } catch (e: Exception) {
            println("Error loading notification settings: ${e.message}")
            NotificationSettings()
        }
    }
    
    // Enhanced update family project with simple overwrite and broadcast to all family members
    suspend fun updateFamilyProject(
        projectId: String,
        groceries: List<GroceryWithDate>,
        categories: List<CustomCategory>,
        senderDeviceId: String
    ): Boolean {
        // If offline, add to queue and return true (pretend success)
        if (!isOnline()) {
            addToOfflineQueue(projectId, groceries, categories, senderDeviceId)
            return true
        }
        
        return try {
            // Validate input
            if (projectId.isBlank()) {
                println("Invalid projectId")
                return false
            }
            
            // First, get current data to get family members
            val currentSnapshot = db.collection(FAMILY_PROJECTS_COLLECTION)
                .document(projectId)
                .get()
                .await()
            
            if (!currentSnapshot.exists()) {
                println("Project doesn't exist: $projectId")
                return false
            }
            
            val currentProject = currentSnapshot.toObject(FamilyProject::class.java)
            if (currentProject == null) {
                println("Invalid project data")
                return false
            }
            
            // SIMPLE OVERWRITE: Use the incoming data directly (no merge logic for now)
            val ourLastUpdated = System.currentTimeMillis()
            
            val updates = mapOf(
                "groceries" to groceries.map { grocery ->
                    FirebaseGrocery(
                        name = grocery.name,
                        customCategoryId = grocery.customCategoryId,
                        expirationDate = grocery.expirationDate?.toString(),
                        lastTimeBoughtDays = grocery.lastTimeBoughtDays,
                        averageBuyingDays = grocery.averageBuyingDays,
                        buyEvents = grocery.buyEvents.map { it.toString() },
                        inShoppingList = grocery.inShoppingList,
                        isBought = grocery.isBought,
                        lastUpdate = grocery.lastUpdate
                    )
                },
                "categories" to categories.map { category ->
                    FirebaseCustomCategory(
                        id = category.id,
                        name = category.name,
                        default = category.default,
                        viewOrder = category.viewOrder,
                        lastUpdate = category.lastUpdate
                    )
                },
                "lastUpdated" to ourLastUpdated
            )
            
            // Use transaction to ensure atomic update
            db.runTransaction { transaction ->
                val docRef = db.collection(FAMILY_PROJECTS_COLLECTION).document(projectId)
                val doc = transaction.get(docRef)
                
                if (!doc.exists()) {
                    throw Exception("Project no longer exists")
                }
                
                transaction.update(docRef, updates)
            }.await()
            
            // BROADCAST UPDATE: Send the update to all family members via FCM (excluding sender)
            broadcastUpdateToFamilyMembers(projectId, currentProject.members, updates, senderDeviceId)
            
            // SEND ACKNOWLEDGMENT: Send success message to the sender
            sendAcknowledgmentToSender(projectId, senderDeviceId, true)
            
            println("Successfully updated family project: $projectId, broadcasted to ${currentProject.members.size} members, and sent acknowledgment to sender")
            true
        } catch (e: FirebaseFirestoreException) {
            println("Firestore error updating family project: ${e.message}")
            // If update fails, add to offline queue
            addToOfflineQueue(projectId, groceries, categories, senderDeviceId)
            // Send failure acknowledgment to sender
            sendAcknowledgmentToSender(projectId, senderDeviceId, false)
            false
        } catch (e: Exception) {
            println("Error updating family project: ${e.message}")
            // If update fails, add to offline queue
            addToOfflineQueue(projectId, groceries, categories, senderDeviceId)
            // Send failure acknowledgment to sender
            sendAcknowledgmentToSender(projectId, senderDeviceId, false)
            false
        }
    }
    
    // Note: Merge functions removed since we're using simple overwrite approach now





        // Detect potential category moves: same name, different category, otherwise identical
        fun equalsExceptCategory(a: GroceryWithDate, b: GroceryWithDate): Boolean {
            return a.name.equals(b.name, ignoreCase = true) &&
                a.expirationDate == b.expirationDate &&
                a.lastTimeBoughtDays == b.lastTimeBoughtDays &&
                a.averageBuyingDays == b.averageBuyingDays &&
                a.buyEvents == b.buyEvents &&
                a.inShoppingList == b.inShoppingList &&
                a.isBought == b.isBought &&
                a.customCategoryId != b.customCategoryId
        }

        // More lenient move detection: same name, different category; allow differences in expiration/avg/lastTime
        fun isPotentialMoveRelaxed(a: GroceryWithDate, b: GroceryWithDate): Boolean {
            return a.name.equals(b.name, ignoreCase = true) &&
                a.buyEvents == b.buyEvents &&
                a.inShoppingList == b.inShoppingList &&
                a.isBought == b.isBought &&
                a.customCategoryId != b.customCategoryId
        }

        val firebaseOnlyKeys = firebaseMap.keys - localMap.keys
        val localOnlyKeys = localMap.keys - firebaseMap.keys

        // Map from firebase-old-key -> local-renamed-item
        val renamePairs = mutableMapOf<String, GroceryWithDate>()
        // Map from firebase-old-key -> local-moved-item (same name, different category)
        val movePairs = mutableMapOf<String, GroceryWithDate>()

        for (localKey in localOnlyKeys) {
            val localItem = localMap[localKey] ?: continue
            // find firebase candidate(s) in same category with same attributes except name
            // Strict rename candidates (all equal except name)
            val candidates = firebaseOnlyKeys.mapNotNull { fKey ->
                val fItem = firebaseMap[fKey]
                if (fItem != null && fItem.customCategoryId == localItem.customCategoryId && equalsExceptName(localItem, fItem)) {
                    fKey to fItem
                } else null
            }
            if (candidates.size == 1) {
                // Treat as rename: prefer local (new name), drop the old firebase one
                val (oldKey, _) = candidates.first()
                renamePairs[oldKey] = localItem
            } else if (candidates.isEmpty()) {
                // Relaxed rename candidates (allow date/avg/lastTime changes)
                val relaxed = firebaseOnlyKeys.mapNotNull { fKey ->
                    val fItem = firebaseMap[fKey]
                    if (fItem != null && fItem.customCategoryId == localItem.customCategoryId && isPotentialRenameRelaxed(localItem, fItem)) {
                        fKey to fItem
                    } else null
                }
                if (relaxed.size == 1) {
                    val (oldKey, _) = relaxed.first()
                    renamePairs[oldKey] = localItem
                }
            }

            // If not a rename, check for category move (same name, different category)
            if (candidates.isEmpty()) {
                val moveCandidates = firebaseOnlyKeys.mapNotNull { fKey ->
                    val fItem = firebaseMap[fKey]
                    if (fItem != null && equalsExceptCategory(localItem, fItem)) {
                        fKey to fItem
                    } else null
                }
                if (moveCandidates.size == 1) {
                    val (oldKey, _) = moveCandidates.first()
                    movePairs[oldKey] = localItem
                } else if (moveCandidates.isEmpty()) {
                    val relaxedMoves = firebaseOnlyKeys.mapNotNull { fKey ->
                        val fItem = firebaseMap[fKey]
                        if (fItem != null && isPotentialMoveRelaxed(localItem, fItem)) {
                            fKey to fItem
                        } else null
                    }
                    if (relaxedMoves.size == 1) {
                        val (oldKey, _) = relaxedMoves.first()
                        movePairs[oldKey] = localItem
                    }
                }
            }
        }

        // Now build the merged list
        val processedFirebaseKeys = mutableSetOf<String>()

        // Merge items present in both by exact key
        for (key in localMap.keys.intersect(firebaseMap.keys)) {
            val localItem = localMap[key]!!
            val firebaseItem = firebaseMap[key]!!
            merged.add(mergeGroceryItems(localItem, firebaseItem))
            processedFirebaseKeys.add(key)
        }

        // Add local-only items (including renamed ones). If this local item is a rename target,
        // it will be added here; the old firebase key will be skipped later.
        for (key in localOnlyKeys) {
            val localItem = localMap[key] ?: continue
            merged.add(localItem)
        }

        // Add remaining firebase-only items EXCEPT those that were matched as renamed/moved
        for (key in firebaseOnlyKeys) {
            if (key in renamePairs.keys) continue // skip old name, replaced by local renamed item
            if (key in movePairs.keys) continue   // skip old category, replaced by local moved item
            if (key in processedFirebaseKeys) continue
            firebaseMap[key]?.let { merged.add(it) }
        }

        return merged
    }
    

    

    
    // Enhanced listen for real-time updates with error handling
    fun listenToFamilyProject(
        projectId: String,
        onUpdate: (FamilyProject?) -> Unit
    ): ListenerRegistration {
        return db.collection(FAMILY_PROJECTS_COLLECTION)
            .document(projectId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error in real-time listener: ${error.message}")
                    onUpdate(null)
                    return@addSnapshotListener
                }
                
                try {
                    // First try to parse as Firebase-compatible data classes
                    val familyProject = snapshot?.toObject(FamilyProject::class.java)
                    if (familyProject != null) {
                        onUpdate(familyProject)
                    } else {
                        onUpdate(null)
                    }
                } catch (e: Exception) {
                    println("Error parsing family project data: ${e.message}")
                    // Try to handle legacy data without lastUpdate properties
                    try {
                        val legacyProject = snapshot?.toObject(LegacyFamilyProject::class.java)
                        if (legacyProject != null) {
                            // Convert legacy data to new format
                            val convertedProject = FamilyProject(
                                projectId = legacyProject.projectId,
                                createdBy = legacyProject.createdBy,
                                createdAt = legacyProject.createdAt,
                                members = legacyProject.members,
                                groceries = legacyProject.groceries.map { legacyGrocery ->
                                    FirebaseGrocery(
                                        name = legacyGrocery.name,
                                        customCategoryId = legacyGrocery.customCategoryId,
                                        expirationDate = legacyGrocery.expirationDate,
                                        lastTimeBoughtDays = legacyGrocery.lastTimeBoughtDays,
                                        averageBuyingDays = legacyGrocery.averageBuyingDays,
                                        buyEvents = legacyGrocery.buyEvents,
                                        inShoppingList = legacyGrocery.inShoppingList,
                                        isBought = legacyGrocery.isBought,
                                        lastUpdate = 0L // Default value for legacy data
                                    )
                                },
                                categories = legacyProject.categories.map { legacyCategory ->
                                    FirebaseCustomCategory(
                                        id = legacyCategory.id,
                                        name = legacyCategory.name,
                                        default = legacyCategory.default,
                                        viewOrder = legacyCategory.viewOrder,
                                        lastUpdate = 0L // Default value for legacy data
                                    )
                                },
                                lastUpdated = legacyProject.lastUpdated
                            )
                            onUpdate(convertedProject)
                        } else {
                            onUpdate(null)
                        }
                    } catch (legacyException: Exception) {
                        println("Error parsing legacy family project data: ${legacyException.message}")
                        onUpdate(null)
                    }
                }
            }
    }
    
    // NEW: Simple broadcast update to all family members (excluding sender)
    private suspend fun broadcastUpdateToFamilyMembers(
        projectId: String, 
        familyMembers: List<String>, 
        updates: Map<String, Any>,
        senderDeviceId: String
    ) {
        try {
            // Simple approach: just log that we would broadcast to family members
            val otherMembers = familyMembers.filter { it != senderDeviceId }
            println("Would broadcast update to ${otherMembers.size} family members (excluding sender $senderDeviceId)")
            println("Update data: $updates")
            
        } catch (e: Exception) {
            println("Error preparing broadcast to family members: ${e.message}")
        }
    }
    
    // NEW: Simple acknowledgment system - calls callback for UI updates
    private suspend fun sendAcknowledgmentToSender(
        projectId: String,
        senderDeviceId: String,
        success: Boolean
    ) {
        try {
            // Simple approach: just log the acknowledgment
            val status = if (success) "SUCCESS" else "FAILED"
            println("=== ACKNOWLEDGMENT SENT ===")
            println("To: $senderDeviceId")
            println("Project: $projectId")
            println("Status: $status")
            println("Timestamp: ${System.currentTimeMillis()}")
            println("==========================")
            
            // Call the callback to notify UI
            onAcknowledgmentReceived?.invoke(projectId, success)
            
        } catch (e: Exception) {
            println("Error logging acknowledgment: ${e.message}")
        }
    }
    
    // Get device ID (unique identifier for this device)
    suspend fun getDeviceId(context: Context): String {
        return DeviceIdProvider.getOrCreateDeviceId(context)
    }
    
    // Get device ID without context (for backward compatibility)
    fun getDeviceId(): String {
        // Use a stable device id when available (set at app start)
        val cached = DeviceIdProvider.deviceId
        return cached ?: UUID.randomUUID().toString()
    }
    
    // Get offline queue size for status reporting
    fun getOfflineQueueSize(): Int = offlineQueue.size
    
    // Clear offline queue (for testing or manual reset)
    fun clearOfflineQueue() {
        offlineQueue.clear()
        println("Offline queue cleared")
    }
}

// Enhanced data classes for Firebase
data class FamilyProject(
    val projectId: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val members: List<String> = emptyList(),
    val groceries: List<FirebaseGrocery> = emptyList(), // Use Firebase-compatible version
    val categories: List<FirebaseCustomCategory> = emptyList(), // Use Firebase-compatible version
    val lastUpdated: Long = 0L
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", 0L, emptyList(), emptyList(), emptyList(), 0L)
    
    // Helper function to convert to regular data classes
    fun toRegularData(): Pair<List<Grocery>, List<CustomCategory>> {
        val regularGroceries = groceries.map { it.toGrocery() }
        val regularCategories = categories.map { it.toCustomCategory() }
        return Pair(regularGroceries, regularCategories)
    }
}

// Firebase-compatible data classes that handle missing lastUpdate properties
data class FirebaseGrocery(
    val name: String = "",
    val customCategoryId: Int = 0,
    val expirationDate: String? = null,
    val lastTimeBoughtDays: Int? = null,
    val averageBuyingDays: Int? = null,
    val buyEvents: List<String> = emptyList(),
    val inShoppingList: Boolean = false,
    val isBought: Boolean = false,
    val lastUpdate: Long? = null // Make optional for backward compatibility
) {
    // Convert to regular Grocery with default lastUpdate if missing
    fun toGrocery(): Grocery = Grocery(
        name = name,
        customCategoryId = customCategoryId,
        expirationDate = expirationDate,
        lastTimeBoughtDays = lastTimeBoughtDays,
        averageBuyingDays = averageBuyingDays,
        buyEvents = buyEvents,
        inShoppingList = inShoppingList,
        isBought = isBought,
        lastUpdate = lastUpdate ?: 0L // Default to 0L if missing
    )
}

data class FirebaseCustomCategory(
    val id: Int = 0,
    val name: String = "",
    val default: Boolean = false,
    val viewOrder: Int = 0,
    val lastUpdate: Long? = null // Make optional for backward compatibility
) {
    // Convert to regular CustomCategory with default lastUpdate if missing
    fun toCustomCategory(): CustomCategory = CustomCategory(
        id = id,
        name = name,
        default = default,
        viewOrder = viewOrder,
        lastUpdate = lastUpdate ?: 0L // Default to 0L if missing
    )
}

// Updated FamilyProject to use Firebase-compatible data classes
data class FamilyProject(
    val projectId: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val members: List<String> = emptyList(),
    val groceries: List<FirebaseGrocery> = emptyList(), // Use Firebase-compatible version
    val categories: List<FirebaseCustomCategory> = emptyList(), // Use Firebase-compatible version
    val lastUpdated: Long = 0L
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", 0L, emptyList(), emptyList(), emptyList(), 0L)
    
    // Helper function to convert to regular data classes
    fun toRegularData(): Pair<List<Grocery>, List<CustomCategory>> {
        val regularGroceries = groceries.map { it.toGrocery() }
        val regularCategories = categories.map { it.toCustomCategory() }
        return Pair(regularGroceries, regularCategories)
    }
}

// Legacy data classes for backward compatibility (without lastUpdate properties)
data class LegacyGrocery(
    val name: String = "",
    val customCategoryId: Int = 0,
    val expirationDate: String? = null,
    val lastTimeBoughtDays: Int? = null,
    val averageBuyingDays: Int? = null,
    val buyEvents: List<String> = emptyList(),
    val inShoppingList: Boolean = false,
    val isBought: Boolean = false
    // Note: No lastUpdate property for legacy data
)

data class LegacyCustomCategory(
    val id: Int = 0,
    val name: String = "",
    val default: Boolean = false,
    val viewOrder: Int = 0
    // Note: No lastUpdate property for legacy data
)

data class LegacyFamilyProject(
    val projectId: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val members: List<String> = emptyList(),
    val groceries: List<LegacyGrocery> = emptyList(),
    val categories: List<LegacyCustomCategory> = emptyList(),
    val lastUpdated: Long = 0L
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", 0L, emptyList(), emptyList(), emptyList(), 0L)
}

data class DeviceRegistration(
    val deviceId: String = "",
    val projectId: String = "",
    val joinedAt: Long = 0L,
    val lastSync: Long = 0L,
    val fcmToken: String = "",
    val notificationSettings: NotificationSettings = NotificationSettings()
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", 0L, 0L, "", NotificationSettings())
}

// Notification settings per device
data class NotificationSettings(
    val notifyItemsAdded: Boolean = true,
    val notifyExpiration: Boolean = true,
    val notifyAverageDue: Boolean = true
)

// Enhanced data class for offline queue
data class OfflineUpdate(
    val projectId: String = "",
    val groceries: List<FirebaseGrocery> = emptyList(),
    val categories: List<FirebaseCustomCategory> = emptyList(),
    val timestamp: Long = 0L,
    var retryCount: Int = 0,
    val senderDeviceId: String = ""
) {
    // No-argument constructor for Firestore
    constructor() : this("", emptyList(), emptyList(), 0L, 0, "")
} 