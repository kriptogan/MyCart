package com.kriptogan.supercart

import java.time.LocalDate
import kotlinx.serialization.Serializable
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.datastore.preferences.preferencesDataStore

// Custom category data class
@Serializable
data class CustomCategory(
    val id: Int = 0,           // Unique identifier
    val name: String = "",       // Display name
    val default: Boolean = false,   // Whether this is a default category
    val viewOrder: Int = 0,      // Order for display
    val lastUpdate: Long = System.currentTimeMillis() // Last update timestamp
) {
    // Validation
    fun isValid(): Boolean = id > 0 && name.isNotBlank()
    
    // Get display name with fallback
    fun getDisplayName(language: String = "iw"): String {
        return if (name.isNotBlank()) name else "Unknown Category"
    }
    
    // Update with new timestamp
    fun withUpdatedTimestamp(): CustomCategory {
        return copy(lastUpdate = System.currentTimeMillis())
    }
}

// Enhanced grocery model
@Serializable
data class Grocery(
    val id: Int = 0, // Unique identifier (0 = not assigned yet, for Firebase backward compatibility)
    val name: String = "", // שם המצרך
    val customCategoryId: Int = 0, // קישור לקטגוריה מותאמת (חובה)
    val expirationDate: String? = null, // תאריך תפוגה (אופציונלי, as ISO string)
    val lastTimeBoughtDays: Int? = null, // מספר ימים מאז הקנייה האחרונה (אופציונלי)
    val averageBuyingDays: Int? = null, // ממוצע ימים בין קניות (אופציונלי)
    val buyEvents: List<String> = emptyList(), // רשימת תאריכי קנייה (ISO)
    val inShoppingList: Boolean = false, // האם המצרך נמצא ברשימת הקניות
    val isBought: Boolean = false, // האם המצרך נרכש
    val lastUpdate: Long = System.currentTimeMillis() // Last update timestamp
) {
    // Validation - allow id = 0 for Firebase backward compatibility
    fun isValid(): Boolean = name.isNotBlank() && customCategoryId > 0
    
    // Check if this grocery has an assigned ID
    fun hasId(): Boolean = id > 0
    
    // Check if item is expired
    fun isExpired(): Boolean {
        val expDate = expirationDate?.let { LocalDate.parse(it) }
        return expDate?.isBefore(LocalDate.now()) == true
    }
    
    // Check if item is expiring soon (within days)
    fun isExpiringSoon(days: Int = 1): Boolean {
        val expDate = expirationDate?.let { LocalDate.parse(it) }
        return expDate?.let { 
            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), it)
            daysUntil <= days.toLong() && daysUntil >= 0
        } ?: false
    }
    
    // Get days until expiration
    fun getDaysUntilExpiration(): Int? {
        val expDate = expirationDate?.let { LocalDate.parse(it) }
        return expDate?.let { 
            java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), it).toInt()
        }
    }
    
    // Check if item should be bought based on average buying pattern
    fun shouldBeBought(): Boolean {
        val avgDays = averageBuyingDays ?: return false
        val lastBuy = buyEvents.maxOrNull()?.let { LocalDate.parse(it) }
        val daysSinceLastBuy = lastBuy?.let { 
            java.time.temporal.ChronoUnit.DAYS.between(it, LocalDate.now()).toInt()
        } ?: return false
        
        return daysSinceLastBuy >= avgDays
    }
    
    // Get days since last purchase
    fun getDaysSinceLastPurchase(): Int? {
        val lastBuy = buyEvents.maxOrNull()?.let { LocalDate.parse(it) }
        return lastBuy?.let { 
            java.time.temporal.ChronoUnit.DAYS.between(it, LocalDate.now()).toInt()
        }
    }
    
    // Get urgency level (0 = normal, 1 = expiring soon, 2 = expired, 3 = overdue for purchase)
    fun getUrgencyLevel(): Int {
        return when {
            isExpired() -> 2
            isExpiringSoon() -> 1
            shouldBeBought() -> 3
            else -> 0
        }
    }
    
    // Get urgency description
    fun getUrgencyDescription(language: String = "iw"): String {
        return when (getUrgencyLevel()) {
            0 -> "Normal"
            1 -> "Expiring Soon"
            2 -> "Expired"
            3 -> "Overdue for Purchase"
            else -> "Unknown"
        }
    }
}

fun Grocery.withLocalDate(): GroceryWithDate = GroceryWithDate(
    id = id,
    name = name,
    customCategoryId = customCategoryId,
    expirationDate = expirationDate?.let { java.time.LocalDate.parse(it) },
    lastTimeBoughtDays = lastTimeBoughtDays,
    averageBuyingDays = averageBuyingDays,
    buyEvents = buyEvents.map { java.time.LocalDate.parse(it) },
    inShoppingList = inShoppingList,
    isBought = isBought,
    lastUpdate = lastUpdate
)

data class GroceryWithDate(
    val id: Int = 0, // Unique identifier (0 = not assigned yet, for Firebase backward compatibility)
    val name: String,
    val customCategoryId: Int,
    val expirationDate: java.time.LocalDate?,
    val lastTimeBoughtDays: Int? = null,
    val averageBuyingDays: Int? = null,
    val buyEvents: List<java.time.LocalDate> = emptyList(),
    val inShoppingList: Boolean = false,
    val isBought: Boolean = false,
    val lastUpdate: Long = System.currentTimeMillis() // Last update timestamp
) {
    // Validation - allow id = 0 for Firebase backward compatibility
    fun isValid(): Boolean = name.isNotBlank() && customCategoryId > 0
    
    // Check if this grocery has an assigned ID
    fun hasId(): Boolean = id > 0
    
    // Check if item is expired
    fun isExpired(): Boolean {
        return expirationDate?.isBefore(java.time.LocalDate.now()) == true
    }
    
    // Check if item is expiring soon (within days)
    fun isExpiringSoon(days: Int = 1): Boolean {
        return expirationDate?.let { 
            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), it)
            daysUntil <= days.toLong() && daysUntil >= 0
        } ?: false
    }
    
    // Get days until expiration
    fun getDaysUntilExpiration(): Int? {
        return expirationDate?.let { 
            java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), it).toInt()
        }
    }
    
    // Check if item should be bought based on average buying pattern
    fun shouldBeBought(): Boolean {
        val avgDays = averageBuyingDays ?: return false
        val lastBuy = buyEvents.maxOrNull()
        val daysSinceLastBuy = lastBuy?.let { 
            java.time.temporal.ChronoUnit.DAYS.between(it, java.time.LocalDate.now()).toInt()
        } ?: return false
        
        return daysSinceLastBuy >= avgDays
    }
    
    // Get days since last purchase
    fun getDaysSinceLastPurchase(): Int? {
        val lastBuy = buyEvents.maxOrNull()
        return lastBuy?.let { 
            java.time.temporal.ChronoUnit.DAYS.between(it, java.time.LocalDate.now()).toInt()
        }
    }
    
    // Get urgency level (0 = normal, 1 = expiring soon, 2 = expired, 3 = overdue for purchase)
    fun getUrgencyLevel(): Int {
        return when {
            isExpired() -> 2
            isExpiringSoon() -> 1
            shouldBeBought() -> 3
            else -> 0
        }
    }
    
    // Get urgency description
    fun getUrgencyDescription(language: String = "iw"): String {
        return when (getUrgencyLevel()) {
            0 -> "Normal"
            1 -> "Expiring Soon"
            2 -> "Expired"
            3 -> "Overdue for Purchase"
            else -> "Unknown"
        }
    }
    
    // Add purchase event
    fun addPurchaseEvent(): GroceryWithDate {
        val today = java.time.LocalDate.now()
        val newBuyEvents = (buyEvents + today).sorted()
        val avg = newBuyEvents.averageDaysBetween()
        return copy(
            lastTimeBoughtDays = 0,
            averageBuyingDays = avg,
            buyEvents = newBuyEvents,
            inShoppingList = false,
            isBought = true,
            lastUpdate = System.currentTimeMillis()
        )
    }
    
    // Toggle shopping list status
    fun toggleShoppingList(): GroceryWithDate {
        return copy(inShoppingList = !inShoppingList, lastUpdate = System.currentTimeMillis())
    }
    
    // Update expiration date
    fun updateExpirationDate(newDate: java.time.LocalDate?): GroceryWithDate {
        return copy(expirationDate = newDate, lastUpdate = System.currentTimeMillis())
    }
    
    // Update category
    fun updateCategory(newCategoryId: Int): GroceryWithDate {
        return copy(customCategoryId = newCategoryId, lastUpdate = System.currentTimeMillis())
    }
    
    // Update name
    fun updateName(newName: String): GroceryWithDate {
        return copy(name = newName, lastUpdate = System.currentTimeMillis())
    }
    
    // Update with new timestamp
    fun withUpdatedTimestamp(): GroceryWithDate {
        return copy(lastUpdate = System.currentTimeMillis())
    }
}

fun GroceryWithDate.toSerializable(): Grocery = Grocery(
    id = id,
    name = name,
    customCategoryId = customCategoryId,
    expirationDate = expirationDate?.toString(),
    lastTimeBoughtDays = lastTimeBoughtDays,
    averageBuyingDays = averageBuyingDays,
    buyEvents = buyEvents.map { it.toString() },
    inShoppingList = inShoppingList,
    isBought = isBought,
    lastUpdate = lastUpdate
)

// Helper functions for Firebase backward compatibility

// Assign IDs to groceries that don't have them (for Firebase data migration)
fun List<Grocery>.assignMissingIds(startingId: Int = 1): List<Grocery> {
    var currentId = startingId
    return map { grocery ->
        if (grocery.id == 0) {
            grocery.copy(id = currentId++)
        } else {
            grocery
        }
    }
}

// Check if any groceries in the list are missing IDs
fun List<Grocery>.hasMissingIds(): Boolean = any { it.id == 0 }

// Object to track and persist the last used ID
private object GroceryIdTracker {
    private var lastUsedId: Int = 0
    private val LAST_USED_ID_KEY = intPreferencesKey("last_used_grocery_id")
    
    fun getAndIncrementId(currentItems: List<GroceryWithDate>, context: android.content.Context): Int {
        // First time: try to get from DataStore, fallback to max ID from items
        if (lastUsedId == 0) {
            lastUsedId = runBlocking {
                try {
                    // Try to get from DataStore
                    val dataStore = context.groceryIdDataStore
                    val storedId = dataStore.data.first()[LAST_USED_ID_KEY] ?: 0
                    maxOf(storedId, currentItems.maxOfOrNull { it.id } ?: 0)
                } catch (e: Exception) {
                    // Fallback to max ID from items if DataStore fails
                    currentItems.maxOfOrNull { it.id } ?: 0
                }
            }
        }
        
        val nextId = ++lastUsedId
        
        // Persist the new ID
        runBlocking {
            try {
                context.groceryIdDataStore.edit { preferences ->
                    preferences[LAST_USED_ID_KEY] = nextId
                }
            } catch (e: Exception) {
                println("Failed to persist last used ID: ${e.message}")
            }
        }
        
        return nextId
    }
}

// Extension property for easy DataStore access
private val android.content.Context.groceryIdDataStore by androidx.datastore.preferences.preferencesDataStore(
    name = "grocery_id_prefs"
)

// Get the next available ID for new groceries (auto-increment)
fun List<GroceryWithDate>.getNextId(context: android.content.Context): Int = 
    GroceryIdTracker.getAndIncrementId(this, context)

fun List<java.time.LocalDate>.averageDaysBetween(): Int? {
    if (size < 2) return null
    val sorted = sorted()
    // Take only the last 4 buy events for more recent pattern analysis
    val recentEvents = if (sorted.size > 4) sorted.takeLast(4) else sorted
    val intervals = recentEvents.zipWithNext { a, b -> java.time.temporal.ChronoUnit.DAYS.between(a, b).toInt() }
    return if (intervals.isNotEmpty()) intervals.sum() / intervals.size else null
}

// Utility functions for grocery management
object GroceryUtils {
    // Filter groceries by urgency
    fun filterByUrgency(groceries: List<GroceryWithDate>, urgencyLevel: Int): List<GroceryWithDate> {
        return groceries.filter { it.getUrgencyLevel() >= urgencyLevel }
    }
    
    // Filter groceries by category
    fun filterByCategory(groceries: List<GroceryWithDate>, categoryId: Int): List<GroceryWithDate> {
        return groceries.filter { it.customCategoryId == categoryId }
    }
    
    // Filter groceries by shopping list status
    fun filterByShoppingList(groceries: List<GroceryWithDate>, inShoppingList: Boolean): List<GroceryWithDate> {
        return groceries.filter { it.inShoppingList == inShoppingList }
    }
    
    // Filter groceries by bought status
    fun filterByBoughtStatus(groceries: List<GroceryWithDate>, isBought: Boolean): List<GroceryWithDate> {
        return groceries.filter { it.isBought == isBought }
    }
    
    // Search groceries by name
    fun searchGroceries(groceries: List<GroceryWithDate>, query: String): List<GroceryWithDate> {
        if (query.isBlank()) return groceries
        return groceries.filter { it.name.contains(query, ignoreCase = true) }
    }
    
    // Get statistics for groceries
    fun getGroceryStats(groceries: List<GroceryWithDate>): GroceryStats {
        val total = groceries.size
        val inShoppingList = groceries.count { it.inShoppingList }
        val expired = groceries.count { it.isExpired() }
        val expiringSoon = groceries.count { it.isExpiringSoon() }
        val overdueForPurchase = groceries.count { it.shouldBeBought() }
        
        return GroceryStats(
            total = total,
            inShoppingList = inShoppingList,
            expired = expired,
            expiringSoon = expiringSoon,
            overdueForPurchase = overdueForPurchase
        )
    }
    
    // Sort groceries by various criteria
    fun sortGroceries(groceries: List<GroceryWithDate>, sortBy: SortCriteria): List<GroceryWithDate> {
        return when (sortBy) {
            SortCriteria.NAME -> groceries.sortedBy { it.name }
            SortCriteria.URGENCY -> groceries.sortedByDescending { it.getUrgencyLevel() }
            SortCriteria.EXPIRATION -> groceries.sortedBy { it.expirationDate ?: java.time.LocalDate.MAX }
            SortCriteria.LAST_PURCHASE -> groceries.sortedBy { it.buyEvents.maxOrNull() ?: java.time.LocalDate.MIN }
            SortCriteria.CATEGORY -> groceries.sortedBy { it.customCategoryId }
        }
    }
}

// Data classes for statistics and sorting
data class GroceryStats(
    val total: Int,
    val inShoppingList: Int,
    val expired: Int,
    val expiringSoon: Int,
    val overdueForPurchase: Int
)

enum class SortCriteria {
    NAME,
    URGENCY,
    EXPIRATION,
    LAST_PURCHASE,
    CATEGORY
} 