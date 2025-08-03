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
                categories = categories
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
    
    // Update family project data
    suspend fun updateFamilyProject(
        projectId: String,
        groceries: List<GroceryWithDate>,
        categories: List<CustomCategory>
    ): Boolean {
        return try {
            val updates = mapOf(
                "groceries" to groceries.map { it.toSerializable() },
                "categories" to categories,
                "lastUpdated" to System.currentTimeMillis()
            )
            
            db.collection(FAMILY_PROJECTS_COLLECTION)
                .document(projectId)
                .update(updates)
                .await()
            
            true
        } catch (e: Exception) {
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