package com.kriptogan.supercart

import com.google.firebase.functions.FirebaseFunctions
import com.kriptogan.supercart.DeviceIdProvider

object FirebaseFunctionsHelper {
    private val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance() }

    fun triggerShoppingListNotification(projectId: String, itemNames: List<String>) {
        val data = hashMapOf(
            "projectId" to projectId,
            "items" to itemNames,
            "senderDeviceId" to (DeviceIdProvider.deviceId ?: "unknown")
        )
        functions
            .getHttpsCallable("notifyShoppingListItemsAdded")
            .call(data)
            .addOnSuccessListener { result ->
                println("Shopping list notification triggered successfully")
            }
            .addOnFailureListener { exception ->
                println("Failed to trigger shopping list notification: ${exception.message}")
            }
    }

    fun checkExpiredItems(projectId: String, onSuccess: (Map<String, Int>) -> Unit, onFailure: (String) -> Unit) {
        val data = hashMapOf(
            "projectId" to projectId
        )
        functions
            .getHttpsCallable("checkExpiredItems")
            .call(data)
            .addOnSuccessListener { result ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val resultData = result.data as? Map<String, Any>
                    if (resultData != null) {
                        val expiredItems = (resultData["expiredItems"] as? Number)?.toInt() ?: 0
                        val expiringItems = (resultData["expiringItems"] as? Number)?.toInt() ?: 0
                        val dueItems = (resultData["dueItems"] as? Number)?.toInt() ?: 0
                        val notificationsSent = (resultData["notificationsSent"] as? Number)?.toInt() ?: 0
                        
                        val response = mapOf<String, Int>(
                            "expiredItems" to expiredItems,
                            "expiringItems" to expiringItems,
                            "dueItems" to dueItems,
                            "notificationsSent" to notificationsSent
                        )
                        onSuccess(response)
                    } else {
                        onFailure("Invalid response format")
                    }
                } catch (e: Exception) {
                    onFailure("Error parsing response: ${e.message}")
                }
            }
            .addOnFailureListener { exception ->
                onFailure("Failed to check expired items: ${exception.message}")
            }
    }
}


