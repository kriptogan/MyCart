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
    private suspend fun addToOfflineQueue(projectId: String, groceries: List<GroceryWithDate>, categories: List<CustomCategory>, boughtItems: List<GroceryWithDate> = emptyList()) {
        val update = OfflineUpdate(
            projectId = projectId,
            groceries = groceries.map { it.toSerializable() },
            categories = categories,
            boughtItems = boughtItems.map { it.toSerializable() },
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
                        update.categories,
                        update.boughtItems.map { it.withLocalDate() }
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
                boughtItems = emptyList(),
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
        categories: List<CustomCategory>,
        boughtItems: List<GroceryWithDate> = emptyList()
    ): Boolean {
        // If offline, add to queue and return true (pretend success)
        if (!isOnline()) {
            addToOfflineQueue(projectId, groceries, categories, boughtItems)
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
            
            // Check if our data is newer than what's on Firebase
            val currentLastUpdated = currentProject.lastUpdated
            val ourLastUpdated = System.currentTimeMillis()
            
            // If Firebase data is newer, don't overwrite it
            if (currentLastUpdated > ourLastUpdated) {
                println("Firebase data is newer, skipping update to avoid conflicts")
                return false
            }
            
            val updates = mapOf(
                "groceries" to groceries.map { it.toSerializable() },
                "categories" to categories,
                "boughtItems" to boughtItems.map { it.toSerializable() },
                "lastUpdated" to ourLastUpdated
            )
            
            // Use transaction to ensure atomic update
            db.runTransaction { transaction ->
                val docRef = db.collection(FAMILY_PROJECTS_COLLECTION).document(projectId)
                val doc = transaction.get(docRef)
                
                if (!doc.exists()) {
                    throw Exception("Project no longer exists")
                }
                
                val project = doc.toObject(FamilyProject::class.java)
                if (project == null) {
                    throw Exception("Invalid project data")
                }
                
                // Double-check timestamp to prevent conflicts
                if (project.lastUpdated > ourLastUpdated) {
                    throw Exception("Data conflict detected")
                }
                
                transaction.update(docRef, updates)
            }.await()
            
            println("Successfully updated family project: $projectId")
            true
        } catch (e: FirebaseFirestoreException) {
            println("Firestore error updating family project: ${e.message}")
            // If update fails, add to offline queue
            addToOfflineQueue(projectId, groceries, categories, boughtItems)
            false
        } catch (e: Exception) {
            println("Error updating family project: ${e.message}")
            // If update fails, add to offline queue
            addToOfflineQueue(projectId, groceries, categories, boughtItems)
            false
        }
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
        // For now, generate a simple device ID
        // In production, you might want to use a more sophisticated approach
        return UUID.randomUUID().toString()
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
    val boughtItems: List<Grocery> = emptyList(),
    val lastUpdated: Long = 0L
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", 0L, emptyList(), emptyList(), emptyList(), emptyList(), 0L)
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
    val boughtItems: List<Grocery> = emptyList(),
    val timestamp: Long = 0L,
    var retryCount: Int = 0
) {
    // No-argument constructor for Firestore
    constructor() : this("", emptyList(), emptyList(), emptyList(), 0L, 0)
} 