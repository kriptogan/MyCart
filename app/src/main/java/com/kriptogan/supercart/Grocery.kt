package com.kriptogan.supercart

import java.time.LocalDate
import kotlinx.serialization.Serializable

// Custom category data class
@Serializable
data class CustomCategory(
    val id: Int,           // Unique identifier
    val name: String,       // Display name
    val default: Boolean,   // Whether this is a default category
    val viewOrder: Int      // Order for display
)

// קטגוריות מצרכים
@Serializable
enum class GroceryCategory(val displayName: String) {
    פירות("פירות"),
    ירקות("ירקות"),
    מאפים_ולחמים("מאפים ולחמים"),
    חטיפים_ומתוקים("חטיפים ומתוקים"),
    דגנים_וקטניות("דגנים וקטניות"),
    שימורים("שימורים"),
    מוצרי_ניקיון("מוצרי נקיון"),
    מזון_לתינוקות("מזון לתינוקות"),
    מזון_יבש("מזון יבש"),
    תבלינים_ורטבים("תבלינים ורטבים"),
    מוצרי_טואלטיקה("מוצרי טואלטיקה"),
    משקאות("משקאות"),
    קפואים("קפואים"),
    מוצרי_חלב("מוצרי חלב"),
    בשר_ודגים("בשר ודגים"),
    מוצרים_לבית("מוצרים לבית"),
    אחר("אחר")
}

// מודל נתונים עבור מצרך
@Serializable
data class Grocery(
    val name: String, // שם המצרך
    val category: GroceryCategory, // קטגוריה
    val expirationDate: String? = null, // תאריך תפוגה (אופציונלי, as ISO string)
    val lastTimeBoughtDays: Int? = null, // מספר ימים מאז הקנייה האחרונה (אופציונלי)
    val averageBuyingDays: Int? = null, // ממוצע ימים בין קניות (אופציונלי)
    val buyEvents: List<String> = emptyList(), // רשימת תאריכי קנייה (ISO)
    val inShoppingList: Boolean = false // האם המצרך נמצא ברשימת הקניות
)

fun Grocery.withLocalDate(): GroceryWithDate = GroceryWithDate(
    name = name,
    category = category,
    expirationDate = expirationDate?.let { java.time.LocalDate.parse(it) },
    lastTimeBoughtDays = lastTimeBoughtDays,
    averageBuyingDays = averageBuyingDays,
    buyEvents = buyEvents.map { java.time.LocalDate.parse(it) },
    inShoppingList = inShoppingList
)

data class GroceryWithDate(
    val name: String,
    val category: GroceryCategory,
    val expirationDate: java.time.LocalDate?,
    val lastTimeBoughtDays: Int? = null,
    val averageBuyingDays: Int? = null,
    val buyEvents: List<java.time.LocalDate> = emptyList(),
    val inShoppingList: Boolean = false // האם המצרך נמצא ברשימת הקניות
)

fun GroceryWithDate.toSerializable(): Grocery = Grocery(
    name = name,
    category = category,
    expirationDate = expirationDate?.toString(),
    lastTimeBoughtDays = lastTimeBoughtDays,
    averageBuyingDays = averageBuyingDays,
    buyEvents = buyEvents.map { it.toString() },
    inShoppingList = inShoppingList
)

fun List<java.time.LocalDate>.averageDaysBetween(): Int? {
    if (size < 2) return null
    val sorted = sorted()
    // Take only the last 4 buy events for more recent pattern analysis
    val recentEvents = if (sorted.size > 4) sorted.takeLast(4) else sorted
    val intervals = recentEvents.zipWithNext { a, b -> java.time.temporal.ChronoUnit.DAYS.between(a, b).toInt() }
    return if (intervals.isNotEmpty()) intervals.sum() / intervals.size else null
} 