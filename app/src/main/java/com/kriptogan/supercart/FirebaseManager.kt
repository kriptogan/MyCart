package com.kriptogan.supercart

import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import android.util.Log

class FirebaseManager {
    companion object {
        private val TAG = "FirebaseManager"
        
        // Initialize Firebase instances lazily to ensure proper initialization
        private val db: FirebaseFirestore by lazy { Firebase.firestore }
        private val auth: FirebaseAuth by lazy { Firebase.auth }
        
                            fun getFirestore(): FirebaseFirestore = db
        
        fun isFirebaseReady(): Boolean {
            return try {
                // Try to access Firestore to see if it's really ready
                try {
                    val testDb = Firebase.firestore
                    Log.d(TAG, "Firestore instance created successfully")
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore not ready: ${e.message}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking Firebase readiness: ${e.message}")
                false
            }
        }
        
        suspend fun waitForFirebaseReady(maxAttempts: Int = 10): Boolean {
            var attempts = 0
            while (attempts < maxAttempts) {
                if (isFirebaseReady()) {
                    Log.d(TAG, "Firebase ready after $attempts attempts")
                    return true
                }
                Log.d(TAG, "Firebase not ready, attempt ${attempts + 1}/$maxAttempts")
                attempts++
                kotlinx.coroutines.delay(500) // Wait 500ms between attempts
            }
            Log.e(TAG, "Firebase not ready after $maxAttempts attempts")
            return false
        }
        
        suspend fun testConnection(): Boolean {
            return try {
                Log.d(TAG, "Testing Firebase connection...")
                
                // Wait for Firebase to be ready
                if (!waitForFirebaseReady()) {
                    Log.e(TAG, "Firebase not ready after waiting")
                    return false
                }
                
                Log.d(TAG, "Firebase is ready, testing Firestore connection...")
                
                // Try to read from a test collection
                val result = db.collection("test").limit(1).get().await()
                Log.d(TAG, "Firestore connection successful, documents count: ${result.size()}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Firebase connection test failed: ${e.message}", e)
                false
            }
        }
    }
}
