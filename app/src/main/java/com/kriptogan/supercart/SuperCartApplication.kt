package com.kriptogan.supercart

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp

class SuperCartApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Create notification channels for Android 8.0+
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Channel for shopping list notifications
            val shoppingListChannel = NotificationChannel(
                CHANNEL_SHOPPING_LIST,
                "Shopping List Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when items are added to shopping list"
                enableLights(true)
                enableVibration(true)
            }
            
            // Channel for expiration notifications
            val expirationChannel = NotificationChannel(
                CHANNEL_EXPIRATION,
                "Item Expiration",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about expired or expiring items"
                enableLights(true)
                enableVibration(true)
            }
            
            // Channel for general notifications
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
                enableLights(true)
                enableVibration(true)
            }
            
            notificationManager.createNotificationChannels(
                listOf(shoppingListChannel, expirationChannel, generalChannel)
            )
        }
    }
    
    companion object {
        const val CHANNEL_SHOPPING_LIST = "shopping_list"
        const val CHANNEL_EXPIRATION = "expiration"
        const val CHANNEL_GENERAL = "general"
    }
}
