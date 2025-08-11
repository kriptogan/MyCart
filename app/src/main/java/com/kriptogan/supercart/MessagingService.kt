package com.kriptogan.supercart

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.kriptogan.supercart.DeviceIdProvider

class MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Update token in backend using coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deviceId = DeviceIdProvider.getOrCreateDeviceId(applicationContext)
                val service = FirebaseService()
                service.updateFcmToken(deviceId, token)
                println("FCM token updated successfully for device: $deviceId")
            } catch (e: Exception) {
                println("Failed to update FCM token: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val messageType = message.data["type"]
        
        when (messageType) {
            "update_acknowledgment" -> {
                // Show sync success toast for acknowledgment messages
                showSyncSuccessToast()
            }
            else -> {
                // Handle other messages with notifications
                val title = message.notification?.title ?: message.data["title"] ?: "SuperCart"
                val body = message.notification?.body ?: message.data["body"] ?: ""
                val channelId = message.data["channelId"] ?: SuperCartApplication.CHANNEL_GENERAL
                
                NotificationHelper.showNotification(
                    applicationContext, 
                    title, 
                    body, 
                    System.currentTimeMillis().toInt(),
                    channelId
                )
            }
        }
    }
    
    private fun showSyncSuccessToast() {
        // Use Handler to show toast on main thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(
                applicationContext,
                "Sync success.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}


