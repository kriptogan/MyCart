package com.kriptogan.supercart

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.random.Random

class FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    
    companion object {
        private const val FAMILY_PROJECTS_COLLECTION = "family_projects"
        private const val DEVICE_REGISTRATIONS_COLLECTION = "device_registrations"
        private const val OFFLINE_QUEUE_COLLECTION = "offline_queue"
    }
    
    // Offline queue for pending updates
    private val offlineQueue = mutableListOf<OfflineUpdate>()
    
    // Check if device is online
    private fun isOnline(): Boolean {
        return try {
            // Simple connectivity check - in production, use NetworkCallback
            true // For now, assume online
        } catch (e: Exception) {
            false
        }
    }
    
    // Add update to offline queue
    private suspend fun addToOfflineQueue(projectId: String, groceries: List<GroceryWithDate>, categories: List<CustomCategory>, boughtItems: List<GroceryWithDate> = emptyList()) {
        val update = OfflineUpdate(
            projectId = projectId,
            groceries = groceries.map { it.toSerializable() },
            categories = categories,
            boughtItems = boughtItems.map { it.toSerializable() },
            timestamp = System.currentTimeMillis()
        )
        offlineQueue.add(update)
        
        // Store in local storage for persistence
        // In a real implementation, you'd use DataStore or Room
        println("Added to offline queue: ${update.timestamp}")
    }
    
    // Process offline queue
    suspend fun processOfflineQueue() {
        if (offlineQueue.isEmpty() || !isOnline()) {
            return
        }
        
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
                    offlineQueue.add(update)
                }
            } catch (e: Exception) {
                // Add back to queue if failed
                offlineQueue.add(update)
                println("Failed to process offline update: ${e.message}")
            }
        }
    }
    
    // Generate unique 8-digit family code
    suspend fun generateFamilyCode(): String {
        var code: String
        do {
            code = generateRandom8DigitCode()
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
            false
        }
    }
    
    // Create new family project
    suspend fun createFamilyProject(
        projectId: String,
        groceries: List<GroceryWithDate>,
        categories: List<CustomCategory>,
        deviceId: String
    ): Boolean {
        return try {
            val familyProject = FamilyProject(
                projectId = projectId,
                createdBy = deviceId,
                createdAt = System.currentTimeMillis(),
                members = listOf(deviceId),
                groceries = groceries.map { it.toSerializable() },
                categories = categories,
                boughtItems = emptyList() // Initialize with empty bought items
            )
            
            db.collection(FAMILY_PROJECTS_COLLECTION)
                .document(projectId)
                .set(familyProject)
                .await()
            
            // Register this device
            registerDevice(deviceId, projectId)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Join existing family project
    suspend fun joinFamilyProject(
        projectId: String,
        deviceId: String
    ): FamilyProject? {
        return try {
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
                    
                    familyProject.copy(members = updatedMembers)
                } else {
                    familyProject
                }
            } else {
                null
            }
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            // Handle registration error
        }
    }
    
    // Update family project data with conflict resolution and offline support
    suspend fun updateFamilyProject(
        projectId: String,
        groceries: List<GroceryWithDate>,
        categories: List<CustomCategory>,
        boughtItems: List<GroceryWithDate> = emptyList() // Add bought items parameter
    ): Boolean {
        // If offline, add to queue and return true (pretend success)
        if (!isOnline()) {
            addToOfflineQueue(projectId, groceries, categories)
            return true
        }
        
        return try {
            // First, get current data to check for conflicts
            val currentSnapshot = db.collection(FAMILY_PROJECTS_COLLECTION)
                .document(projectId)
                .get()
                .await()
            
            if (!currentSnapshot.exists()) {
                return false // Project doesn't exist
            }
            
            val currentProject = currentSnapshot.toObject(FamilyProject::class.java)
            if (currentProject == null) {
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
            
            true
        } catch (e: Exception) {
            println("Error updating family project: ${e.message}")
            // If update fails, add to offline queue
            addToOfflineQueue(projectId, groceries, categories)
            false
        }
    }
    
    // Listen for real-time updates
    fun listenToFamilyProject(
        projectId: String,
        onUpdate: (FamilyProject?) -> Unit
    ): ListenerRegistration {
        return db.collection(FAMILY_PROJECTS_COLLECTION)
            .document(projectId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onUpdate(null)
                    return@addSnapshotListener
                }
                
                val familyProject = snapshot?.toObject(FamilyProject::class.java)
                onUpdate(familyProject)
            }
    }
    
    // Get device ID (unique identifier for this device)
    fun getDeviceId(): String {
        // For now, generate a simple device ID
        // In production, you might want to use a more sophisticated approach
        return UUID.randomUUID().toString()
    }
}

// Data classes for Firebase
data class FamilyProject(
    val projectId: String = "",
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val members: List<String> = emptyList(),
    val groceries: List<Grocery> = emptyList(),
    val categories: List<CustomCategory> = emptyList(),
    val boughtItems: List<Grocery> = emptyList(), // Add bought items state
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

// Data class for offline queue
data class OfflineUpdate(
    val projectId: String = "",
    val groceries: List<Grocery> = emptyList(),
    val categories: List<CustomCategory> = emptyList(),
    val boughtItems: List<Grocery> = emptyList(), // Add bought items state
    val timestamp: Long = 0L
) {
    // No-argument constructor for Firestore
    constructor() : this("", emptyList(), emptyList(), emptyList(), 0L)
} 