package com.kriptogan.supercart

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseService {
    private val db = FirebaseFirestore.getInstance()
    
    companion object {
        private const val TEST_COLLECTION = "device_registrations"
    }
    
    // Simple connection test - just try to read from the device_registrations collection
    suspend fun testConnection(): Boolean {
        return try {
            val result = db.collection(TEST_COLLECTION).limit(1).get().await()
            println("Firebase connection test successful, documents count: ${result.size()}")
            true
        } catch (e: Exception) {
            println("Firebase connection test failed: ${e.message}")
            false
        }
    }
    
    // Get Firestore instance for future use
    fun getFirestore(): FirebaseFirestore = db
}
