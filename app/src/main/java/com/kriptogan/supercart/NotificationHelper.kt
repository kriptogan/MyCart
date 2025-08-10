package com.kriptogan.supercart

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID_ALERTS = "supercart_alerts"
    private const val CHANNEL_NAME_ALERTS = "SuperCart Alerts"
    private const val CHANNEL_DESC_ALERTS = "Family and item alerts"
    
    private const val CHANNEL_ID_DAILY = "supercart_daily"
    private const val CHANNEL_NAME_DAILY = "SuperCart Daily Reminders"
    private const val CHANNEL_DESC_DAILY = "Daily reminders for expiration and buying due"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                CHANNEL_NAME_ALERTS,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC_ALERTS
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            
            // Create daily reminders channel
            val dailyChannel = NotificationChannel(
                CHANNEL_ID_DAILY,
                CHANNEL_NAME_DAILY,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC_DAILY
            }
            manager.createNotificationChannel(dailyChannel)
        }
    }

    fun showNotification(context: Context, title: String, message: String, notificationId: Int, channelId: String = CHANNEL_ID_ALERTS) {
        ensureChannel(context)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
    
    fun showDailyNotification(context: Context, title: String, message: String, notificationId: Int) {
        ensureChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_DAILY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
    
    fun showExpirationNotification(context: Context, expiredItems: List<String>, expiringItems: List<String>) {
        if (expiredItems.isEmpty() && expiringItems.isEmpty()) return
        
        val title = "SuperCart - Item Expiration Alert"
        val message = buildString {
            if (expiredItems.isNotEmpty()) {
                append("Expired: ${expiredItems.joinToString(", ")}")
            }
            if (expiredItems.isNotEmpty() && expiringItems.isNotEmpty()) {
                append("\n")
            }
            if (expiringItems.isNotEmpty()) {
                append("Expiring soon: ${expiringItems.joinToString(", ")}")
            }
        }
        
        showDailyNotification(context, title, message, 1001)
    }
    
    fun showAverageBuyingDueNotification(context: Context, dueItems: List<String>) {
        if (dueItems.isEmpty()) return
        
        val title = "SuperCart - Items Due for Purchase"
        val message = "Time to buy: ${dueItems.joinToString(", ")}"
        
        showDailyNotification(context, title, message, 1002)
    }
}


