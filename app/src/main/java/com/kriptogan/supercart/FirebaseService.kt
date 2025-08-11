package com.kriptogan.supercart

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.delay

class FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    
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
    private suspend fun addToOfflineQueue(projectId: String, groceries: List<GroceryWithDate>, categories: List<CustomCategory>) {
        val update = OfflineUpdate(
            projectId = projectId,
            groceries = groceries.map { it.toSerializable() },
            categories = categories,
            timestamp = System.currentTimeMillis(),
            retryCount = 0
        )
        offlineQueue.add(update)
        
        // Store in local storage for persistence
        println("Added to offline queue: ${update.timestamp} (${offlineQueue.size} total)")
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
                        update.groceries.map { it.withLocalDate() }, 
                        update.categories
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
                groceries = groceries.map { it.toSerializable() },
                categories = categories,
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
                    familyProject.copy(members = updatedMembers)
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


    
    // Enhanced update family project with better conflict resolution and offline support
    suspend fun updateFamilyProject(
        projectId: String,
        groceries: List<GroceryWithDate>,
        categories: List<CustomCategory>
    ): Boolean {
        // If offline, add to queue and return true (pretend success)
        if (!isOnline()) {
            addToOfflineQueue(projectId, groceries, categories)
            return true
        }
        
        return try {
            // Validate input
            if (projectId.isBlank()) {
                println("Invalid projectId")
                return false
            }
            
            // First, get current data to check for conflicts
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
            
            // IMPROVED CONFLICT RESOLUTION: Merge changes instead of simple timestamp comparison
            // Handle groceries without IDs from Firebase (backward compatibility)
            val groceriesWithIds = if (currentProject.groceries.hasMissingIds()) {
                println("DEBUG: Found groceries without IDs in Firebase, assigning IDs...")
                currentProject.groceries.assignMissingIds()
            } else {
                currentProject.groceries
            }
            
            val currentGroceries = groceriesWithIds.map { it.withLocalDate() }
            val mergedGroceries = mergeGroceryLists(currentGroceries, groceries)
            val mergedCategories = mergeCategories(currentProject.categories, categories)
            
            val ourLastUpdated = System.currentTimeMillis()
            
            val updates = mapOf(
                "groceries" to mergedGroceries.map { it.toSerializable() },
                "categories" to mergedCategories,
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
            
            println("Successfully updated family project: $projectId")
            true
        } catch (e: FirebaseFirestoreException) {
            println("Firestore error updating family project: ${e.message}")
            // If update fails, add to offline queue
            addToOfflineQueue(projectId, groceries, categories)
            false
        } catch (e: Exception) {
            println("Error updating family project: ${e.message}")
            // If update fails, add to offline queue
            addToOfflineQueue(projectId, groceries, categories)
            false
        }
    }
    
    // NEW: Smart merge function for groceries that preserves user changes
    private fun mergeGroceryLists(
        firebaseGroceries: List<GroceryWithDate>,
        localGroceries: List<GroceryWithDate>
    ): List<GroceryWithDate> {
        val merged = mutableListOf<GroceryWithDate>()

        // Maps keyed by current identity (name + category)
        val localMap = localGroceries.associateBy { "${it.name}_${it.customCategoryId}" }
        val firebaseMap = firebaseGroceries.associateBy { "${it.name}_${it.customCategoryId}" }

        // Detect potential renames: local item that doesn't exist in Firebase by key,
        // but has a Firebase counterpart with the same category and identical attributes except name
        data class Key(val name: String, val categoryId: Int)

        fun equalsExceptName(a: GroceryWithDate, b: GroceryWithDate): Boolean {
            return a.customCategoryId == b.customCategoryId &&
                a.expirationDate == b.expirationDate &&
                a.lastTimeBoughtDays == b.lastTimeBoughtDays &&
                a.averageBuyingDays == b.averageBuyingDays &&
                a.buyEvents == b.buyEvents &&
                a.inShoppingList == b.inShoppingList &&
                a.isBought == b.isBought
        }

        // More lenient rename detection: allow differences in expiration/avg/lastTime; require same category,
        // same buyEvents, and same boolean flags to reduce false positives
        fun isPotentialRenameRelaxed(a: GroceryWithDate, b: GroceryWithDate): Boolean {
            return a.customCategoryId == b.customCategoryId &&
                a.buyEvents == b.buyEvents &&
                a.inShoppingList == b.inShoppingList &&
                a.isBought == b.isBought
        }

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
    
    // NEW: Enhanced intelligent merge of individual grocery items with timestamp-based protection
    private fun mergeGroceryItems(local: GroceryWithDate, firebase: GroceryWithDate): GroceryWithDate {
        val currentTime = System.currentTimeMillis()
        
        // For shopping list status, ALWAYS prefer local changes (user actions)
        val finalInShoppingList = if (local.inShoppingList != firebase.inShoppingList) {
            // If there's a conflict in shopping list status, prefer local (user action)
            println("DEBUG: Shopping list conflict resolved - preferring local (${local.inShoppingList}) over Firebase (${firebase.inShoppingList})")
            local.inShoppingList
        } else {
            local.inShoppingList
        }
        
        // For bought status, ALWAYS prefer local changes (user actions)
        val finalIsBought = if (local.isBought != firebase.isBought) {
            // If there's a conflict in bought status, prefer local (user action)
            println("DEBUG: Bought status conflict resolved - preferring local (${local.isBought}) over Firebase (${firebase.isBought})")
            local.isBought
        } else {
            local.isBought
        }
        
        // For other properties, prefer the most recent change with timestamp consideration
        val finalExpirationDate = when {
            local.expirationDate != firebase.expirationDate -> {
                // If local has a more recent expiration date or firebase has none, prefer local
                if (local.expirationDate != null && (firebase.expirationDate == null || 
                    local.expirationDate.isAfter(firebase.expirationDate))) {
                    println("DEBUG: Expiration date conflict resolved - preferring local date")
                    local.expirationDate
                } else {
                    println("DEBUG: Expiration date conflict resolved - preferring Firebase date")
                    firebase.expirationDate
                }
            }
            else -> local.expirationDate
        }
        
        // For buy events, merge and sort with timestamp-based deduplication
        val mergedBuyEvents = (local.buyEvents + firebase.buyEvents).distinct().sorted()
        
        // For average buying days, prefer the more recent calculation
        val finalAverageBuyingDays = when {
            local.averageBuyingDays != firebase.averageBuyingDays -> {
                // Prefer the one with more buy events (more recent data)
                if (local.buyEvents.size >= firebase.buyEvents.size) {
                    println("DEBUG: Average buying days conflict resolved - preferring local (more buy events)")
                    local.averageBuyingDays
                } else {
                    println("DEBUG: Average buying days conflict resolved - preferring Firebase (more buy events)")
                    firebase.averageBuyingDays
                }
            }
            else -> local.averageBuyingDays
        }
        
        // NEW: Enhanced logging for debugging sync issues
        if (local.inShoppingList != firebase.inShoppingList || local.isBought != firebase.isBought) {
            println("DEBUG: Critical field conflict detected for '${local.name}' at $currentTime")
            println("DEBUG: Local - inShoppingList: ${local.inShoppingList}, isBought: ${local.isBought}")
            println("DEBUG: Firebase - inShoppingList: ${firebase.inShoppingList}, isBought: ${firebase.isBought}")
            println("DEBUG: Final - inShoppingList: $finalInShoppingList, isBought: $finalIsBought")
        }
        
        return GroceryWithDate(
            id = local.id, // Preserve the local ID
            name = local.name,
            customCategoryId = local.customCategoryId,
            expirationDate = finalExpirationDate,
            lastTimeBoughtDays = local.lastTimeBoughtDays,
            averageBuyingDays = finalAverageBuyingDays,
            buyEvents = mergedBuyEvents,
            inShoppingList = finalInShoppingList,
            isBought = finalIsBought,
            lastUpdate = System.currentTimeMillis()
        )
    }
    
    // NEW: Smart merge function for categories
    private fun mergeCategories(firebaseCategories: List<CustomCategory>, localCategories: List<CustomCategory>): List<CustomCategory> {
        // Categories are more complex - prefer local order but preserve Firebase customizations
        val merged = mutableListOf<CustomCategory>()
        val processedIds = mutableSetOf<Int>()
        
        // Add local categories first (user's order takes priority)
        localCategories.forEach { localCategory ->
            processedIds.add(localCategory.id)
            merged.add(localCategory)
        }
        
        // Add Firebase categories that don't exist locally
        firebaseCategories.forEach { firebaseCategory ->
            if (!processedIds.contains(firebaseCategory.id)) {
                merged.add(firebaseCategory)
            }
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
                    val familyProject = snapshot?.toObject(FamilyProject::class.java)
                    onUpdate(familyProject)
                } catch (e: Exception) {
                    println("Error parsing family project data: ${e.message}")
                    onUpdate(null)
                }
            }
    }
    
    // Get device ID (unique identifier for this device)
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
    val groceries: List<Grocery> = emptyList(),
    val categories: List<CustomCategory> = emptyList(),
    val lastUpdated: Long = 0L
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", 0L, emptyList(), emptyList(), emptyList(), 0L)
}

data class DeviceRegistration(
    val deviceId: String = "",
    val projectId: String = "",
    val joinedAt: Long = 0L,
    val lastSync: Long = 0L
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", 0L, 0L)
}

// Enhanced data class for offline queue
data class OfflineUpdate(
    val projectId: String = "",
    val groceries: List<Grocery> = emptyList(),
    val categories: List<CustomCategory> = emptyList(),
    val timestamp: Long = 0L,
    var retryCount: Int = 0
) {
    // No-argument constructor for Firestore
    constructor() : this("", emptyList(), emptyList(), 0L, 0)
} 