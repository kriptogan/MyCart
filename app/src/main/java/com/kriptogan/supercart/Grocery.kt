package com.kriptogan.supercart

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.serialization.Serializable

// Custom category data class
@Serializable
data class CustomCategory(
    val uuid: String = UUID.randomUUID().toString(), // New UUID identifier
    val id: Int,           // Legacy integer identifier (for migration)
    val name: String,       // Display name
    val default: Boolean,   // Whether this is a default category
    val viewOrder: Int,     // Order for display
    val lastUpdate: String = LocalDateTime.now().toString() // Last update timestamp (ISO string)
)

// מודל נתונים עבור מצרך
@Serializable
data class Grocery(
    val uuid: String = UUID.randomUUID().toString(), // New UUID identifier
    val name: String, // שם המצרך
    val customCategoryId: Int, // קישור לקטגוריה מותאמת (חובה - legacy)
    val customCategoryUuid: String? = null, // New UUID reference to category
    val expirationDate: String? = null, // תאריך תפוגה (אופציונלי, as ISO string)
    val lastTimeBoughtDays: Int? = null, // מספר ימים מאז הקנייה האחרונה (אופציונלי)
    val averageBuyingDays: Int? = null, // ממוצע ימים בין קניות (אופציונלי)
    val buyEvents: List<String> = emptyList(), // רשימת תאריכי קנייה (ISO)
    val inShoppingList: Boolean = false, // האם המצרך נמצא ברשימת הקניות
    val lastUpdate: String = LocalDateTime.now().toString() // Last update timestamp (ISO string)
)

fun Grocery.withLocalDate(): GroceryWithDate = GroceryWithDate(
    uuid = uuid,
    name = name,
    customCategoryId = customCategoryId,
    customCategoryUuid = customCategoryUuid,
    expirationDate = expirationDate?.let { java.time.LocalDate.parse(it) },
    lastTimeBoughtDays = lastTimeBoughtDays,
    averageBuyingDays = averageBuyingDays,
    buyEvents = buyEvents.map { java.time.LocalDate.parse(it) },
    inShoppingList = inShoppingList,
    lastUpdate = LocalDateTime.parse(lastUpdate)
)

data class GroceryWithDate(
    val uuid: String = UUID.randomUUID().toString(), // New UUID identifier
    val name: String,
    val customCategoryId: Int, // Legacy integer reference (for migration)
    val customCategoryUuid: String? = null, // New UUID reference to category
    val expirationDate: java.time.LocalDate?,
    val lastTimeBoughtDays: Int? = null,
    val averageBuyingDays: Int? = null,
    val buyEvents: List<java.time.LocalDate> = emptyList(),
    val inShoppingList: Boolean = false, // האם המצרך נמצא ברשימת הקניות
    val lastUpdate: LocalDateTime = LocalDateTime.now() // Last update timestamp
)

fun GroceryWithDate.toSerializable(): Grocery = Grocery(
    uuid = uuid,
    name = name,
    customCategoryId = customCategoryId,
    customCategoryUuid = customCategoryUuid,
    expirationDate = expirationDate?.toString(),
    lastTimeBoughtDays = lastTimeBoughtDays,
    averageBuyingDays = averageBuyingDays,
    buyEvents = buyEvents.map { it.toString() },
    inShoppingList = inShoppingList,
    lastUpdate = lastUpdate.toString()
)

fun List<java.time.LocalDate>.averageDaysBetween(): Int? {
    if (size < 2) return null
    val sorted = sorted()
    // Take only the last 4 buy events for more recent pattern analysis
    val recentEvents = if (sorted.size > 4) sorted.takeLast(4) else sorted
    val intervals = recentEvents.zipWithNext { a, b -> java.time.temporal.ChronoUnit.DAYS.between(a, b).toInt() }
    return if (intervals.isNotEmpty()) intervals.sum() / intervals.size else null
}

// Migration helper functions
fun CustomCategory.ensureUuid(): CustomCategory {
    return if (uuid.isBlank()) {
        this.copy(uuid = UUID.randomUUID().toString())
    } else {
        this
    }
}

fun Grocery.ensureUuid(): Grocery {
    return if (uuid.isBlank()) {
        this.copy(uuid = UUID.randomUUID().toString())
    } else {
        this
    }
}

fun GroceryWithDate.ensureUuid(): GroceryWithDate {
    return if (uuid.isBlank()) {
        this.copy(uuid = UUID.randomUUID().toString())
    } else {
        this
    }
}

// Helper function to link grocery to category by UUID
fun Grocery.linkToCategoryUuid(categories: List<CustomCategory>): Grocery {
    val category = categories.find { it.id == this.customCategoryId }
    return this.copy(customCategoryUuid = category?.uuid)
}

fun GroceryWithDate.linkToCategoryUuid(categories: List<CustomCategory>): GroceryWithDate {
    val category = categories.find { it.id == this.customCategoryId }
    return this.copy(customCategoryUuid = category?.uuid)
} 