package com.kriptogan.supercart

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Utility class for device identification and management
 */
object DeviceUtils {
    
    /**
     * Get or generate a unique device ID for this device
     * Uses Android ID as base, falls back to generated UUID if needed
     */
    fun getDeviceId(context: Context): String {
        val sharedPrefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        val existingId = sharedPrefs.getString("device_id", null)
        
        if (existingId != null) {
            return existingId
        }
        
        // Try to get Android ID first
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        
        val deviceId = if (androidId != null && androidId != "9774d56d682e549c") {
            // Use Android ID if it's valid (not the generic emulator ID)
            androidId
        } else {
            // Generate a new UUID if Android ID is not available or is generic
            UUID.randomUUID().toString()
        }
        
        // Save the device ID for future use
        sharedPrefs.edit().putString("device_id", deviceId).apply()
        
        return deviceId
    }
    
    /**
     * Check if the current device ID is valid
     */
    fun isValidDeviceId(deviceId: String): Boolean {
        return deviceId.isNotBlank() && deviceId.length >= 8
    }
    
    /**
     * Generate a new device ID (useful for testing or reset scenarios)
     */
    fun generateNewDeviceId(context: Context): String {
        val newId = UUID.randomUUID().toString()
        context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("device_id", newId)
            .apply()
        return newId
    }
}
