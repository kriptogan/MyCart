package com.kriptogan.supercart

import java.time.LocalDate
import kotlinx.serialization.Serializable

// קטגוריות מצרכים
@Serializable
enum class GroceryCategory(val displayName: String) {
    פירות("פירות"),
    ירקות("ירקות"),
    מוצרי_חלב("מוצרי חלב"),
    בשר_ודגים("בשר ודגים"),
    מאפים_ולחמים("מאפים ולחמים"),
    קפואים("קפואים"),
    משקאות("משקאות"),
    חטיפים_ומתוקים("חטיפים ומתוקים"),
    דגנים_וקטניות("דגנים וקטניות"),
    תבלינים_ורטבים("תבלינים ורטבים"),
    מוצרי_ניקיון("מוצרי ניקיון"),
    מוצרי_טואלטיקה("מוצרי טואלטיקה"),
    מזון_יבש("מזון יבש"),
    שימורים("שימורים"),
    מזון_לתינוקות("מזון לתינוקות"),
    מזון_לחיות_מחמד("מזון לחיות מחמד"),
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
    val averageBuyingDays: Int? = null // ממוצע ימים בין קניות (אופציונלי)
)

fun Grocery.withLocalDate(): GroceryWithDate = GroceryWithDate(
    name = name,
    category = category,
    expirationDate = expirationDate?.let { java.time.LocalDate.parse(it) },
    lastTimeBoughtDays = lastTimeBoughtDays,
    averageBuyingDays = averageBuyingDays
)

data class GroceryWithDate(
    val name: String,
    val category: GroceryCategory,
    val expirationDate: java.time.LocalDate?,
    val lastTimeBoughtDays: Int? = null,
    val averageBuyingDays: Int? = null
)

fun GroceryWithDate.toSerializable(): Grocery = Grocery(
    name = name,
    category = category,
    expirationDate = expirationDate?.toString(),
    lastTimeBoughtDays = lastTimeBoughtDays,
    averageBuyingDays = averageBuyingDays
) 