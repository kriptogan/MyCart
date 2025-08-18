package com.kriptogan.supercart

import androidx.compose.runtime.Composable

// Custom string resource system
object StringResources {
    private val hebrewStrings = mapOf(
        "home" to "בית",
        "shopping_list" to "רשימת קניות",
        "add_item" to "הוסף מצרך",
        "menu" to "תפריט",
        "search_placeholder" to "חפש מצרך...",
        "languages" to "שפות",
        "show_version" to "הצג גרסה",
        "choose_language" to "בחר שפה",
        "hebrew" to "עברית",
        "english" to "English",
        "russian" to "Русский",
        "close" to "סגור",
        "cancel" to "ביטול",
        "save" to "שמור",
        "edit" to "ערוך",
        "delete" to "מחק",
        "add" to "הוסף",
        "remove_from_list" to "הסר",
        "bought" to "נרכש",
        "add_to_bought" to "הוסף לקנוי",
        "bought_items" to "פריטים שנרכשו:",
        "finish_shopping" to "סיים קניות",
        "finish_shopping_with_count" to "סיים קניות (%d פריטים)",
        "confirm_finish_shopping" to "אישור סיום קניות",
        "confirm_finish_shopping_message" to "האם אתה בטוח שברצונך לסיים את הקניות? %d פריטים יירשמו כנרכשו.",
        "manage_categories" to "ניהול קטגוריות",
        "import_shopping_list" to "ייבוא רשימת קניות",
        "add_item_title" to "הוסף מצרך",
        "edit_item_title" to "ערוך מצרך",
        "item_name" to "שם המצרך",
        "choose_category" to "בחר קטגוריה",
        "select_category" to "בחר קטגוריה",
        "expiration_date" to "בחר תאריך תפוגה (אופציונלי)",
        "confirm_delete" to "אישור מחיקה",
        "confirm_delete_message" to "האם אתה בטוח שברצונך למחוק?",
        "delete_category" to "מחק קטגוריה",
        "delete_category_message" to "הקטגוריה '%s' מכילה פריטים. מה ברצונך לעשות?",
        "delete_category_confirm" to "האם אתה בטוח שברצונך למחוק את הקטגוריה '%s'?",
        "delete_all_items" to "מחק את כל הפריטים",
        "move_to_other" to "העבר ל'אחר'",
        "create_new_category" to "צור קטגוריה חדשה",
        "new_category" to "קטגוריה חדשה",
        "category_name" to "שם הקטגוריה",
        "buy_history" to "היסטוריית קניות - %s",
        "buy_history_button" to "היסטוריית קניות (%d קניות)",
        "days_ago" to "%d ימים",
        "add_to_shopping_list" to "הוסף לרשימת קניות",
        "add_to_shopping_list_message" to "האם ברצונך להוסיף את '%s' לרשימת הקניות?",
        "duplicate_item_title" to "פריט כבר קיים",
        "duplicate_item_message" to "הפריט '%s' כבר קיים ברשימה. מה ברצונך לעשות?",
        "back" to "חזור",
        "show" to "הצג",
        "import_confirm_title" to "אישור ייבוא רשימה",
        "import_confirm_message" to "%d פריטים חדשים יתווספו למערכת. האם תרצה שיוצבו גם ברשימת הקניות?",
        "yes" to "כן",
        "no" to "לא",
        "return_to_shopping_list" to "החזר לרשימת קניות",
        "categories_list" to "רשימת קטגוריות",
        "move_up" to "העלה",
        "move_down" to "הורד",
        "add_list" to "הוסף רשימה",
        "add_items_instructions" to "הוסף פריטים. כל פריט בשורה נפרדת.",
        "edit_category_name" to "ערוך שם קטגוריה",
        "show_items" to "הצג פריטים",
        "items_need_attention" to "פריטים שדורשים תשומת לב",
        "items_need_attention_message" to "יש פריטים שפג תוקפם, עומדים לפוג, או עבר ממוצע הקנייה שלהם. האם ברצונך לראות אותם?",
        "show_expiring_items" to "הצג רק מוצרים שפג תוקפם, עומדים לפוג, או עבר ממוצע קנייה",
        "sharing_group" to "קבוצת שיתוף",
        "create" to "צור",
        // Category translations
        "אחר" to "אחר",
        "פירות" to "פירות",
        "ירקות" to "ירקות",
        "מאפים ולחמים" to "מאפים ולחמים",
        "חטיפים ומתוקים" to "חטיפים ומתוקים",
        "דגנים וקטניות" to "דגנים וקטניות",
        "שימורים" to "שימורים",
        "חד פעמי" to "חד פעמי",
        "מוצרי נקיון" to "מוצרי נקיון",
        "מוצרים לתינוקות" to "מוצרים לתינוקות",
        "מזון יבש" to "מזון יבש",
        "תבלינים ורטבים" to "תבלינים ורטבים",
        "מוצרי טואלטיקה" to "מוצרי טואלטיקה",
        "משקאות" to "משקאות",
        "קפואים" to "קפואים",
        "מוצרי חלב" to "מוצרי חלב",
        "בשר ודגים" to "בשר ודגים",
        "מוצרים לבית" to "מוצרים לבית"
    )
    
    private val englishStrings = mapOf(
        "home" to "Home",
        "shopping_list" to "Shopping List",
        "add_item" to "Add Item",
        "menu" to "Menu",
        "search_placeholder" to "Search items...",
        "languages" to "Languages",
        "show_version" to "Show Version",
        "choose_language" to "Choose Language",
        "hebrew" to "עברית",
        "english" to "English",
        "russian" to "Русский",
        "close" to "Close",
        "cancel" to "Cancel",
        "save" to "Save",
        "edit" to "Edit",
        "delete" to "Delete",
        "add" to "Add",
        "remove_from_list" to "Remove",
        "bought" to "Bought",
        "add_to_bought" to "Add to Bought",
        "bought_items" to "Bought Items:",
        "finish_shopping" to "Finish Shopping",
        "finish_shopping_with_count" to "Finish Shopping (%d items)",
        "confirm_finish_shopping" to "Confirm Finish Shopping",
        "confirm_finish_shopping_message" to "Are you sure you want to finish shopping? %d items will be recorded as bought.",
        "manage_categories" to "Manage Categories",
        "import_shopping_list" to "Import Shopping List",
        "add_item_title" to "Add Item",
        "edit_item_title" to "Edit Item",
        "item_name" to "Item Name",
        "choose_category" to "Choose Category",
        "select_category" to "Select Category",
        "expiration_date" to "Choose expiration date (optional)",
        "confirm_delete" to "Confirm Delete",
        "confirm_delete_message" to "Are you sure you want to delete?",
        "delete_category" to "Delete Category",
        "delete_category_message" to "The category '%s' contains items. What would you like to do?",
        "delete_category_confirm" to "Are you sure you want to delete the category '%s'?",
        "delete_all_items" to "Delete All Items",
        "move_to_other" to "Move to 'Other'",
        "create_new_category" to "Create New Category",
        "new_category" to "New Category",
        "category_name" to "Category Name",
        "buy_history" to "Buy History - %s",
        "buy_history_button" to "Buy History (%d purchases)",
        "days_ago" to "%d days",
        "add_to_shopping_list" to "Add to Shopping List",
        "add_to_shopping_list_message" to "Do you want to add '%s' to the shopping list?",
        "duplicate_item_title" to "Item Already Exists",
        "duplicate_item_message" to "The item '%s' already exists in the list. What would you like to do?",
        "back" to "Back",
        "show" to "Show",
        "import_confirm_title" to "Confirm Import List",
        "import_confirm_message" to "The following %d new items will be added to the system. Do you want them to be placed in the shopping list as well?",
        "yes" to "Yes",
        "no" to "No",
        "return_to_shopping_list" to "Return to Shopping List",
        "categories_list" to "Categories List",
        "move_up" to "Move Up",
        "move_down" to "Move Down",
        "add_list" to "Add List",
        "add_items_instructions" to "Add items. Each item on a separate line.",
        "edit_category_name" to "Edit Category Name",
        "show_items" to "Show Items",
        "items_need_attention" to "Items Need Attention",
        "items_need_attention_message" to "There are items that have expired, are about to expire, or have exceeded their average buying period. Would you like to see them?",
        "show_expiring_items" to "Show only items that have expired, are about to expire, or have exceeded their average buying period",
        "sharing_group" to "Sharing Group",
        "create" to "Create",
        // Category translations
        "אחר" to "Other",
        "פירות" to "Fruits",
        "ירקות" to "Vegetables",
        "מאפים ולחמים" to "Breads & Pastries",
        "חטיפים ומתוקים" to "Snacks & Sweets",
        "דגנים וקטניות" to "Grains & Legumes",
        "שימורים" to "Canned Goods",
        "חד פעמי" to "Disposable",
        "מוצרי נקיון" to "Cleaning Products",
        "מוצרים לתינוקות" to "Baby Products",
        "מזון יבש" to "Dry Food",
        "תבלינים ורטבים" to "Spices & Sauces",
        "מוצרי טואלטיקה" to "Toiletries",
        "משקאות" to "Beverages",
        "קפואים" to "Frozen",
        "מוצרי חלב" to "Dairy Products",
        "בשר ודגים" to "Meat & Fish",
        "מוצרים לבית" to "Home Products"
    )
    
    private val russianStrings = mapOf(
        "home" to "Главная",
        "shopping_list" to "Список покупок",
        "add_item" to "Добавить товар",
        "menu" to "Меню",
        "search_placeholder" to "Поиск товаров...",
        "languages" to "Языки",
        "show_version" to "Показать версию",
        "choose_language" to "Выберите язык",
        "hebrew" to "עברית",
        "english" to "English",
        "russian" to "Русский",
        "close" to "Закрыть",
        "cancel" to "Отмена",
        "save" to "Сохранить",
        "edit" to "Редактировать",
        "delete" to "Удалить",
        "add" to "Добавить",
        "remove_from_list" to "Удалить",
        "bought" to "Куплено",
        "add_to_bought" to "Добавить в купленное",
        "bought_items" to "Купленные товары:",
        "finish_shopping" to "Завершить покупки",
        "finish_shopping_with_count" to "Завершить покупки (%d товаров)",
        "confirm_finish_shopping" to "Подтвердить завершение покупок",
        "confirm_finish_shopping_message" to "Вы уверены, что хотите завершить покупки? %d товаров будут записаны как купленные.",
        "manage_categories" to "Управление категориями",
        "import_shopping_list" to "Импорт списка покупок",
        "add_item_title" to "Добавить товар",
        "edit_item_title" to "Редактировать товар",
        "item_name" to "Название товара",
        "choose_category" to "Выберите категорию",
        "select_category" to "Выберите категорию",
        "expiration_date" to "Выберите дату истечения срока (необязательно)",
        "confirm_delete" to "Подтвердить удаление",
        "confirm_delete_message" to "Вы уверены, что хотите удалить?",
        "delete_category" to "Удалить категорию",
        "delete_category_message" to "Категория '%s' содержит товары. Что вы хотите сделать?",
        "delete_category_confirm" to "Вы уверены, что хотите удалить категорию '%s'?",
        "delete_all_items" to "Удалить все товары",
        "move_to_other" to "Переместить в 'Другое'",
        "create_new_category" to "Создать новую категорию",
        "new_category" to "Новая категория",
        "category_name" to "Название категории",
        "buy_history" to "История покупок - %s",
        "buy_history_button" to "История покупок (%d покупок)",
        "days_ago" to "%d дней",
        "add_to_shopping_list" to "Добавить в список покупок",
        "add_to_shopping_list_message" to "Хотите добавить '%s' в список покупок?",
        "duplicate_item_title" to "Товар уже существует",
        "duplicate_item_message" to "Товар '%s' уже существует в списке. Что вы хотите сделать?",
        "back" to "Назад",
        "show" to "Показать",
        "import_confirm_title" to "Подтвердить импорт списка",
        "import_confirm_message" to "Следующие %d новых товаров будут добавлены в систему. Хотите ли вы также разместить их в списке покупок?",
        "yes" to "Да",
        "no" to "Нет",
        "return_to_shopping_list" to "Вернуть в список покупок",
        "categories_list" to "Список категорий",
        "move_up" to "Поднять",
        "move_down" to "Опустить",
        "add_list" to "Добавить список",
        "add_items_instructions" to "Добавьте товары. Каждый товар с новой строки.",
        "edit_category_name" to "Редактировать название категории",
        "show_items" to "Показать товары",
        "items_need_attention" to "Товары требуют внимания",
        "items_need_attention_message" to "Есть товары, срок годности которых истек, истекает или превышен средний период покупки. Хотите их увидеть?",
        "show_expiring_items" to "Показать только товары, срок годности которых истек, истекает или превышен средний период покупки",
        "sharing_group" to "Группа обмена",
        "create" to "Создать",
        // Category translations
        "אחר" to "Другое",
        "פירות" to "Фрукты",
        "ירקות" to "Овощи",
        "מאפים ולחמים" to "Хлеб и выпечка",
        "חטיפים ומתוקים" to "Закуски и сладости",
        "דגנים וקטניות" to "Зерновые и бобовые",
        "Консервы" to "שימורים",
        "חד פעמי" to "Одноразовые",
        "מוצרי נקיון" to "Моющие средства",
        "מוצרים לתינוקות" to "Детские товары",
        "מזון יבש" to "Сухие продукты",
        "תבלינים ורטבים" to "Специи и соусы",
        "מוצרי טואלטיקה" to "Туалетные принадлежности",
        "משקאות" to "Напитки",
        "קפואים" to "Замороженные",
        "מוצרי חלב" to "Молочные продукты",
        "בשר ודגים" to "Мясо и рыба",
        "מוצרים לבית" to "Товары для дома"
    )
    
    fun getString(key: String, language: String, vararg args: Any): String {
        val strings = when (language) {
            "iw" -> hebrewStrings
            "en" -> englishStrings
            "ru" -> russianStrings
            else -> hebrewStrings
        }
        
        val baseString = strings[key] ?: key
        return if (args.isNotEmpty()) {
            var result = baseString
            args.forEachIndexed { index, arg ->
                // Replace %s with string arguments and %d with numeric arguments
                if (arg is Number) {
                    result = result.replaceFirst("%d", arg.toString())
                } else {
                    result = result.replaceFirst("%s", arg.toString())
                }
            }
            result
        } else {
            baseString
        }
    }
    
    fun translateCategoryName(categoryName: String, language: String): String {
        return getString(categoryName, language)
    }
    
    fun getOriginalCategoryName(translatedName: String, language: String): String {
        // Reverse mapping to get original Hebrew name
        return when (language) {
            "en" -> {
                when (translatedName) {
                    "Other" -> "אחר"
                    "Fruits" -> "פירות"
                    "Vegetables" -> "ירקות"
                    "Breads & Pastries" -> "מאפים ולחמים"
                    "Snacks & Sweets" -> "חטיפים ומתוקים"
                    "Grains & Legumes" -> "דגנים וקטניות"
                    "Canned Goods" -> "שימורים"
                    "Disposable" -> "חד פעמי"
                    "Cleaning Products" -> "מוצרי נקיון"
                    "Baby Products" -> "מוצרים לתינוקות"
                    "Dry Food" -> "מזון יבש"
                    "Spices & Sauces" -> "תבלינים ורטבים"
                    "Toiletries" -> "מוצרי טואלטיקה"
                    "Beverages" -> "משקאות"
                    "Frozen" -> "קפואים"
                    "Dairy Products" -> "מוצרי חלב"
                    "Meat & Fish" -> "בשר ודגים"
                    "Home Products" -> "מוצרים לבית"
                    else -> translatedName // Return as-is if not found in mapping
                }
            }
            "ru" -> {
                when (translatedName) {
                    "Другое" -> "אחר"
                    "Фрукты" -> "פירות"
                    "Овощи" -> "ירקות"
                    "Хлеб и выпечка" -> "מאפים ולחמים"
                    "Закуски и сладости" -> "חטיפים ומתוקים"
                    "Зерновые и бобовые" -> "דגנים וקטניות"
                    "Консервы" -> "שימורים"
                    "Одноразовые" -> "חד פעמי"
                    "Моющие средства" -> "מוצרי נקיון"
                    "Детские товары" -> "מוצרים לתינוקות"
                    "Сухие продукты" -> "מזון יבש"
                    "Специи и соусы" -> "תבלינים ורטבים"
                    "Туалетные принадлежности" -> "מוצרי טואלטיקה"
                    "Напитки" -> "משקאות"
                    "Замороженные" -> "קפואים"
                    "Молочные продукты" -> "מוצרי חלב"
                    "Мясо и рыба" -> "בשר ודגים"
                    "Товары для дома" -> "מוצרים לבית"
                    else -> translatedName // Return as-is if not found in mapping
                }
            }
            else -> translatedName // For Hebrew, return as-is
        }
    }
}

@Composable
fun localizedString(key: String, language: String, vararg args: Any): String {
    return StringResources.getString(key, language, *args)
}

@Composable
fun localizedCategoryName(categoryName: String, language: String): String {
    return StringResources.translateCategoryName(categoryName, language)
}

@Composable
fun getOriginalCategoryName(translatedName: String, language: String): String {
    return StringResources.getOriginalCategoryName(translatedName, language)
}
