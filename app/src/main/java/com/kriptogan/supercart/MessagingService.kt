package com.kriptogan.supercart

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Update token in backend
        val service = FirebaseService()
        service.updateFcmToken(service.getDeviceId(), token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "SuperCart"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        NotificationHelper.showNotification(applicationContext, title, body, System.currentTimeMillis().toInt())
    }
}


