package com.kriptogan.supercart

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

object DeviceIdProvider {
    private const val DEVICE_ID_KEY = "device_id"
    
    @Volatile
    var deviceId: String? = null
    
    private val Context.deviceIdDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_id")
    
    suspend fun getOrCreateDeviceId(context: Context): String {
        // Return cached value if available
        deviceId?.let { return it }
        
        // Try to get from DataStore
        val preferences = context.deviceIdDataStore.data.first()
        val storedId = preferences[stringPreferencesKey(DEVICE_ID_KEY)]
        
        if (storedId != null) {
            deviceId = storedId
            return storedId
        }
        
        // Generate new ID and store it
        val newId = UUID.randomUUID().toString()
        context.deviceIdDataStore.edit { prefs ->
            prefs[stringPreferencesKey(DEVICE_ID_KEY)] = newId
        }
        
        deviceId = newId
        return newId
    }
    
    suspend fun clearDeviceId(context: Context) {
        context.deviceIdDataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(DEVICE_ID_KEY))
        }
        deviceId = null
    }
}


