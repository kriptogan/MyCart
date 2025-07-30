package com.kriptogan.supercart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kriptogan.supercart.ui.theme.SuperCartTheme
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import com.kriptogan.supercart.Grocery
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import com.kriptogan.supercart.GroceryWithDate
import com.kriptogan.supercart.toSerializable
import com.kriptogan.supercart.withLocalDate
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.Serializable
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material.icons.filled.Warning
import java.time.temporal.ChronoUnit
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.staticCompositionLocalOf

// Custom string resource system
object StringResources {
    private val hebrewStrings = mapOf(
        "home" to "בית",
        "shopping_list" to "רשימת קניות",
        "add_item" to "הוסף מצרך",
        "menu" to "תפריט",
        "search_placeholder" to "חפש מצרך...",
        "languages" to "שפות",
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
        "expiration_date" to "בחר תאריך תפוגה (אופציונלי)"
    )
    
    private val englishStrings = mapOf(
        "home" to "Home",
        "shopping_list" to "Shopping List",
        "add_item" to "Add Item",
        "menu" to "Menu",
        "search_placeholder" to "Search items...",
        "languages" to "Languages",
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
        "expiration_date" to "Choose expiration date (optional)"
    )
    
    private val russianStrings = mapOf(
        "home" to "Главная",
        "shopping_list" to "Список покупок",
        "add_item" to "Добавить товар",
        "menu" to "Меню",
        "search_placeholder" to "Поиск товаров...",
        "languages" to "Языки",
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
        "expiration_date" to "Выберите дату истечения срока (необязательно)"
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
            baseString.replace("%d", args[0].toString())
        } else {
            baseString
        }
    }
}

@Composable
fun localizedString(key: String, language: String, vararg args: Any): String {
    return StringResources.getString(key, language, *args)
}

data class TabItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun CustomStatusBar() {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf("") }
    var batteryLevel by remember { mutableStateOf(0) }
    
    // Update time every second
    DisposableEffect(Unit) {
        val timeUpdateRunnable = object : Runnable {
            override fun run() {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                currentTime = sdf.format(Date())
            }
        }
        
        // Initial update
        timeUpdateRunnable.run()
        
        // Set up periodic updates
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val updateTime = object : Runnable {
            override fun run() {
                timeUpdateRunnable.run()
                handler.postDelayed(this, 1000) // Update every second
            }
        }
        handler.post(updateTime)
        
        // Battery level receiver
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    batteryLevel = if (level != -1 && scale != -1) {
                        (level * 100 / scale.toFloat()).toInt()
                    } else 0
                }
            }
        }
        
        context.registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        
        onDispose {
            handler.removeCallbacksAndMessages(null)
            context.unregisterReceiver(batteryReceiver)
        }
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4CAF50), // Green gradient
                        Color(0xFF66BB6A)
                    )
                )
            ),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Battery (right side in RTL)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$batteryLevel%",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp, 12.dp)
                        .background(
                            color = Color.White,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                        )
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Clock (left side in RTL)
            Text(
                text = currentTime,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force Hebrew locale and RTL layout
        val locale = java.util.Locale("iw", "IL") // Hebrew, Israel
        java.util.Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)
        
        // Hide status bar and make app full screen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // Initialize default custom categories and migrate existing data
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            initializeDefaultCustomCategories()
            migrateGroceriesToCustomCategories()
        }
        
        enableEdgeToEdge()
        setContent {
            SuperCartTheme {
                SuperCartApp()
            }
        }
    }
}

@Composable
fun SuperCartApp() {
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    
    // Language state at the app level
    var selectedLanguage by remember { mutableStateOf("iw") } // Current language (iw=Hebrew, en=English, ru=Russian)
    var currentLocale by remember { mutableStateOf(java.util.Locale("iw")) } // Current locale for RTL/LTR support
    var languageChangeKey by remember { mutableStateOf(0) } // Force recomposition when language changes
    
    // Load selected language from DataStore
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val savedLanguage = context.languageDataStore.data.first()[LANGUAGE_KEY]
        selectedLanguage = savedLanguage ?: "iw" // Default to Hebrew
        currentLocale = java.util.Locale(selectedLanguage)
    }
    
    // Save selected language to DataStore
    LaunchedEffect(selectedLanguage) {
        context.languageDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = selectedLanguage
        }
    }
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        TabItem(localizedString("home", selectedLanguage), Icons.Default.Home),
        TabItem(localizedString("shopping_list", selectedLanguage), Icons.Default.ShoppingCart)
    )
    var groceries by remember { mutableStateOf(listOf<GroceryWithDate>()) }
    val scope = rememberCoroutineScope()

    // Custom categories state
    var customCategories by remember { mutableStateOf<List<CustomCategory>>(emptyList()) }
    var categoryOrder by remember { mutableStateOf<List<Int>?>(null) }

    // Load custom categories and order on first composition
    LaunchedEffect(Unit) {
        customCategories = context.customCategoriesDataStore.data.first()
        val saved = context.categoryOrderDataStore.data.first()[CATEGORY_ORDER_KEY]
        categoryOrder = saved?.split(",")?.mapNotNull { it.toIntOrNull() }
    }

    // Compute ordered categories - ensure all categories are included
    val currentCategoryOrder = categoryOrder
    var orderedCategories by remember { mutableStateOf<List<CustomCategory>>(emptyList()) }
    
    // Update ordered categories when customCategories or categoryOrder changes
    LaunchedEffect(customCategories, categoryOrder) {
        orderedCategories = if (currentCategoryOrder != null && currentCategoryOrder.isNotEmpty()) {
            // Use saved order, but ensure all categories are included
            val orderedFromPrefs = currentCategoryOrder.mapNotNull { id -> customCategories.find { it.id == id } }
            val missingCategories = customCategories.filter { cat -> !orderedFromPrefs.any { it.id == cat.id } }
            orderedFromPrefs + missingCategories.sortedBy { it.viewOrder }
        } else {
            // Use default order
            customCategories.sortedBy { it.viewOrder }
        }
    }

    // Load groceries from DataStore on first composition
    LaunchedEffect(Unit) {
        val loaded = context.groceryDataStore.data.first().map { it.withLocalDate() }
        groceries = loaded
    }
    // Save groceries to DataStore whenever they change
    LaunchedEffect(groceries) {
        context.groceryDataStore.updateData { groceries.map { it.toSerializable() } }
    }
    
    // Save custom categories to DataStore whenever they change
    LaunchedEffect(customCategories) {
        context.customCategoriesDataStore.updateData { customCategories }
    }
    
    // Save category order to DataStore whenever it changes
    LaunchedEffect(customCategories) {
        val categoryOrderString = customCategories.sortedBy { it.viewOrder }.map { it.id }.joinToString(",")
        context.categoryOrderDataStore.edit { preferences ->
            preferences[CATEGORY_ORDER_KEY] = categoryOrderString
        }
    }

    // Helper function to get shopping list items by filtering inShoppingList = true
    val shoppingListItems = remember(groceries) {
        groceries.filter { it.inShoppingList }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CustomStatusBar()
            Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp), // Space for custom status bar
            containerColor = Color(0xFFF5F5F5), // Light gray background
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF4CAF50), // Green navigation bar
                    contentColor = Color.White
                ) {
                    tabs.forEachIndexed { index, tabItem ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                if (tabItem.title == localizedString("shopping_list", selectedLanguage) && shoppingListItems.isNotEmpty()) {
                                    BadgedBox(badge = {
                                        Badge(
                                            containerColor = Color(0xFFFF5722)
                                        ) {
                                            Text(
                                                shoppingListItems.size.toString(),
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }) {
                                        Icon(
                                            imageVector = tabItem.icon,
                                            contentDescription = tabItem.title,
                                            tint = if (selectedTab == index) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = tabItem.icon,
                                        contentDescription = tabItem.title,
                                        tint = if (selectedTab == index) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            label = { 
                                Text(
                                    text = tabItem.title,
                                    color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.7f)
                                ) 
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        shoppingList = shoppingListItems,
                        groceries = groceries,
                        onUpdateGroceries = { groceries = it },
                        onAddToShoppingList = { grocery ->
                            groceries = groceries.map {
                                if (it.name == grocery.name && it.customCategoryId == grocery.customCategoryId) {
                                    it.copy(inShoppingList = !it.inShoppingList)
                                } else {
                                    it
                                }
                            }
                        },
                        orderedCategories = orderedCategories,
                        customCategories = customCategories,
                        onUpdateCategories = { categories -> 
                            customCategories = categories
                            // The orderedCategories will be updated automatically by the LaunchedEffect
                        },
                        scope = scope,
                        selectedLanguage = selectedLanguage,
                        onLanguageChange = { newLanguage ->
                            selectedLanguage = newLanguage
                            currentLocale = java.util.Locale(newLanguage)
                            languageChangeKey++
                        }
                    )
                    1 -> ShoppingListScreen(
                        shoppingList = shoppingListItems,
                        groceries = groceries,
                        onUpdateGroceries = { groceries = it },
                        onRemove = { grocery ->
                            groceries = groceries.map {
                                if (it.name == grocery.name && it.customCategoryId == grocery.customCategoryId) {
                                    it.copy(inShoppingList = false)
                                } else {
                                    it
                                }
                            }
                        },
                        onBuy = { grocery ->
                            groceries = groceries.map {
                                if (it.name == grocery.name && it.customCategoryId == grocery.customCategoryId) {
                                    val today = LocalDate.now()
                                    val newBuyEvents = (it.buyEvents + today).sorted()
                                    val avg = newBuyEvents.averageDaysBetween()
                                    it.copy(
                                        lastTimeBoughtDays = 0,
                                        averageBuyingDays = avg,
                                        buyEvents = newBuyEvents,
                                        inShoppingList = false
                                    )
                                } else {
                                    it
                                }
                            }
                        },
                        orderedCategories = orderedCategories,
                        customCategories = customCategories
                    )
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    shoppingList: List<GroceryWithDate>,
    groceries: List<GroceryWithDate>,
    onUpdateGroceries: (List<GroceryWithDate>) -> Unit,
    onAddToShoppingList: (GroceryWithDate) -> Unit,
    orderedCategories: List<CustomCategory>,
    customCategories: List<CustomCategory>,
    onUpdateCategories: (List<CustomCategory>) -> Unit,
    scope: CoroutineScope,
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf(-1) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showExpiringOnly by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("") }
    var showCategoriesList by remember { mutableStateOf(false) } // For categories list dialog
    var expanded by remember { mutableStateOf(false) } // For category dropdown
    var showEditCategoryDialog by remember { mutableStateOf(false) } // For editing category name
    var editingCategory by remember { mutableStateOf<CustomCategory?>(null) } // Category being edited
    var editingCategoryName by remember { mutableStateOf("") } // New name for the category
    var showDeleteCategoryDialog by remember { mutableStateOf(false) } // For delete category confirmation
    var categoryToDelete by remember { mutableStateOf<CustomCategory?>(null) } // Category to be deleted
    var showCreateCategoryDialog by remember { mutableStateOf(false) } // For creating new category
    var newCategoryName by remember { mutableStateOf("") } // Name for new category
    var showMenu by remember { mutableStateOf(false) } // For hamburger menu
    var showBuyHistoryDialog by remember { mutableStateOf(false) } // For showing buy history
    var selectedGroceryForHistory by remember { mutableStateOf<GroceryWithDate?>(null) } // Grocery to show history for
    var showAddToShoppingListConfirm by remember { mutableStateOf(false) } // For confirmation dialog when adding new item
    var showAlertNotification by remember { mutableStateOf(false) } // For alert notification popup
    var showLanguageSelection by remember { mutableStateOf(false) } // For language selection dialog
    
    // Update configuration when locale changes
    val configuration = LocalConfiguration.current
    val layoutDirection = if (selectedLanguage == "iw") LayoutDirection.Rtl else LayoutDirection.Ltr

    // Helper to check if a grocery is expired or expiring soon
    fun isExpiringOrExpired(grocery: GroceryWithDate): Boolean {
        val exp = grocery.expirationDate ?: return false
        return try {
            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), exp)
            daysUntil <= 1L
        } catch (e: Exception) { false }
    }
    val hasExpiring = groceries.any { grocery ->
        val expDate = grocery.expirationDate
        val isExpiringOrExpired = expDate != null && try {
            val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expDate)
            daysUntil <= 1L
        } catch (e: Exception) { false }
        val daysSinceLastBuy = grocery.buyEvents.maxOrNull()?.let { date -> ChronoUnit.DAYS.between(date, LocalDate.now()).toInt() } ?: -1
        val shouldHighlightYellow = !isExpiringOrExpired && grocery.averageBuyingDays != null && daysSinceLastBuy >= (grocery.averageBuyingDays!! - 1)
        isExpiringOrExpired || shouldHighlightYellow
    }

    // Fields for new/edit grocery
    var name by remember { mutableStateOf("") }
    var selectedCustomCategoryId by remember { mutableStateOf(1) } // Default to "אחר"
    var expirationDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var inShoppingList by remember { mutableStateOf(false) } // Shopping list toggle state

    // State for expanded/collapsed categories
    val categoryExpansion = remember { mutableStateMapOf<Int, Boolean>() }
    
    // Initialize expansion state for all categories
    LaunchedEffect(orderedCategories) {
        orderedCategories.forEach { cat ->
            if (categoryExpansion[cat.id] == null) {
                categoryExpansion[cat.id] = true
            }
        }
    }
    
    // Debug: Print current state
    LaunchedEffect(groceries, orderedCategories) {
        println("DEBUG: Total groceries: ${groceries.size}")
        println("DEBUG: Ordered categories: ${orderedCategories.map { "${it.name} (${it.id})" }}")
        groceries.forEach { grocery ->
            val category = customCategories.find { it.id == grocery.customCategoryId }
            println("DEBUG: Grocery '${grocery.name}' -> Category: ${category?.name ?: "UNKNOWN"} (ID: ${grocery.customCategoryId})")
        }
    }
    
    // Show alert notification when app starts and there are items needing attention
    LaunchedEffect(hasExpiring) {
        if (hasExpiring && !showAlertNotification) {
            showAlertNotification = true
        }
    }
    


    fun openEditDialog(index: Int, grocery: GroceryWithDate) {
        name = grocery.name
        selectedCustomCategoryId = grocery.customCategoryId
        expirationDate = grocery.expirationDate
        inShoppingList = grocery.inShoppingList
        editIndex = index
        isEditMode = true
        showDialog = true
    }

    fun shouldShowInAlertFilter(grocery: GroceryWithDate): Boolean {
        val expDate = grocery.expirationDate
        val isExpiringOrExpired = expDate != null && try {
            val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expDate)
            daysUntil <= 1L
        } catch (e: Exception) { false }
        val daysSinceLastBuy = grocery.buyEvents.maxOrNull()?.let { date -> ChronoUnit.DAYS.between(date, LocalDate.now()).toInt() } ?: -1
        val shouldHighlightYellow = !isExpiringOrExpired && grocery.averageBuyingDays != null && daysSinceLastBuy >= (grocery.averageBuyingDays!! - 1)
        return isExpiringOrExpired || shouldHighlightYellow
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Top buttons row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            name = if (searchQuery.isNotBlank()) searchQuery else ""
                            selectedCustomCategoryId = 1 // Default to "אחר"
                            expirationDate = null
                            inShoppingList = false
                            isEditMode = false
                            showDialog = true
                        },
                        modifier = Modifier
                            .background(
                                color = Color(0xFF4CAF50),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = localizedString("add_item", selectedLanguage),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .background(
                                color = Color(0xFF607D8B),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "תפריט",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { if (hasExpiring) showExpiringOnly = !showExpiringOnly },
                        enabled = hasExpiring,
                        modifier = Modifier
                            .background(
                                color = if (hasExpiring) Color(0xFFFF9800) else Color(0xFFBDBDBD),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "הצג רק מוצרים שפג תוקפם, עומדים לפוג, או עבר ממוצע קנייה",
                            tint = Color.White
                        )
                    }
                }
            }
            
            // Hamburger menu dropdown
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Invisible anchor for dropdown positioning
                    Box(
                        modifier = Modifier.size(0.dp)
                    ) {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.width(200.dp)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        localizedString("manage_categories", selectedLanguage),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    ) 
                                },
                                onClick = {
                                    showCategoriesList = true
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        localizedString("import_shopping_list", selectedLanguage),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    ) 
                                },
                                onClick = {
                                    showNotesDialog = true
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        localizedString("languages", selectedLanguage),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    ) 
                                },
                                onClick = {
                                    showLanguageSelection = true
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }
            
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(localizedString("search_placeholder", selectedLanguage)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color(0xFFBDBDBD),
                        focusedLabelColor = Color(0xFF4CAF50)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
            }
            orderedCategories.forEach { category ->
                val itemsInCategory = groceries.withIndex()
                    .filter { it.value.customCategoryId == category.id && it.value.name.contains(searchQuery, ignoreCase = true) }
                    .filter { !showExpiringOnly || shouldShowInAlertFilter(it.value) }
                
                // Debug: Print category filtering results
                if (itemsInCategory.isNotEmpty()) {
                    println("DEBUG: Category '${category.name}' (${category.id}) has ${itemsInCategory.size} items")
                }
                
                if (itemsInCategory.isNotEmpty()) {
                    item(key = category.id) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            border = CardDefaults.outlinedCardBorder(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Column {
                                // Header (clickable for expand/collapse)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { categoryExpansion[category.id] = !(categoryExpansion[category.id] ?: true) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (categoryExpansion[category.id] == true) "▲" else "▼",
                                        fontSize = 18.sp
                                    )
                                }
                                Divider()
                                if (categoryExpansion[category.id] == true) {
                                    itemsInCategory.forEach { indexedGrocery ->
                                        val isExpiringOrExpired = indexedGrocery.value.expirationDate != null &&
                                            try {
                                                val expDate = indexedGrocery.value.expirationDate
                                                val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), expDate)
                                                daysUntil <= 1L
                                            } catch (e: Exception) { false }
                                        val daysSinceLastBuy = indexedGrocery.value.buyEvents.maxOrNull()?.let { ChronoUnit.DAYS.between(it, LocalDate.now()).toInt() } ?: -1
                                        val shouldHighlightYellow = !isExpiringOrExpired && indexedGrocery.value.averageBuyingDays != null && daysSinceLastBuy >= (indexedGrocery.value.averageBuyingDays!! - 1)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                                .background(
                                                    when {
                                                        isExpiringOrExpired -> Color(0xFFFFEBEE) // Lighter red
                                                        shouldHighlightYellow -> Color(0xFFFFF8E1) // Lighter yellow
                                                        else -> Color(0xFFF8F9FA) // Very light gray
                                                    },
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = indexedGrocery.value.name,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { openEditDialog(indexedGrocery.index, indexedGrocery.value) },
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = stringResource(R.string.edit)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onAddToShoppingList(indexedGrocery.value) },
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ShoppingCart,
                                                    contentDescription = if (indexedGrocery.value.inShoppingList) stringResource(R.string.remove_from_list) else stringResource(R.string.add_item),
                                                    tint = if (indexedGrocery.value.inShoppingList) Color.Green else Color.Unspecified
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Button(onClick = { showDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "ביטול",
                                tint = Color.White
                            )
                        }
                        Row {
                            if (isEditMode) {
                                Button(
                                    onClick = { showDeleteConfirm = true },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Color.Red
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "מחק",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Button(onClick = {
                                if (name.isNotBlank()) {
                                    if (isEditMode && editIndex >= 0) {
                                        val updatedGroceries = groceries.toMutableList().also {
                                            it[editIndex] = it[editIndex].copy(
                                                name = name,
                                                customCategoryId = selectedCustomCategoryId,
                                                expirationDate = expirationDate,
                                                inShoppingList = inShoppingList
                                            )
                                        }
                                        onUpdateGroceries(updatedGroceries)
                                        name = ""
                                        selectedCustomCategoryId = 1 // Default to "אחר"
                                        expirationDate = null
                                        inShoppingList = false
                                        showDialog = false
                                    } else {
                                        // For new items, show confirmation dialog
                                        showAddToShoppingListConfirm = true
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = if (isEditMode) "שמור" else "הוסף",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                },
                title = { Text(if (isEditMode) localizedString("edit_item_title", selectedLanguage) else localizedString("add_item_title", selectedLanguage)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(localizedString("item_name", selectedLanguage)) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Category dropdown
                        Box {
                            val selectedCategory = customCategories.find { it.id == selectedCustomCategoryId }
                            Button(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedCategory?.name ?: localizedString("choose_category", selectedLanguage))
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                customCategories.sortedBy { it.viewOrder }.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            selectedCustomCategoryId = cat.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Expiration date picker
                        Button(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (expirationDate != null) expirationDate.toString() else localizedString("expiration_date", selectedLanguage))
                        }
                        if (showDatePicker) {
                            val datePickerState = rememberDatePickerState()
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    Button(onClick = {
                                        val millis = datePickerState.selectedDateMillis
                                        expirationDate = millis?.let {
                                            LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                                        }
                                        showDatePicker = false
                                    }) { Text(stringResource(R.string.save)) }
                                },
                                dismissButton = {
                                    Button(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
                                }
                            ) {
                                DatePicker(state = datePickerState)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Buy history button (only show in edit mode)
                        if (isEditMode && editIndex >= 0) {
                            val currentGrocery = groceries[editIndex]
                            if (currentGrocery.buyEvents.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        selectedGroceryForHistory = currentGrocery
                                        showBuyHistoryDialog = true
                                    }
                                ) {
                                    Text("היסטוריית קניות (${currentGrocery.buyEvents.size} קניות)")
                                }
                            }
                        }
                    }
                }
            )
        }
        if (showDeleteConfirm && isEditMode && editIndex >= 0) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Button(onClick = { showDeleteConfirm = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "ביטול",
                                tint = Color.White
                            )
                        }
                        Button(
                            onClick = {
                                val updatedGroceries = groceries.toMutableList().also { it.removeAt(editIndex) }
                                onUpdateGroceries(updatedGroceries)
                                showDeleteConfirm = false
                                showDialog = false
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "מחק",
                                tint = Color.White
                            )
                        }
                    }
                },
                title = { Text("אישור מחיקה") },
                text = { Text("האם אתה בטוח שברצונך למחוק?") }
            )
        }
        
        // Notes dialog
        if (showNotesDialog) {
            AlertDialog(
                onDismissRequest = { showNotesDialog = false },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Button(onClick = { 
                            notesText = ""
                            showNotesDialog = false 
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = Color.White
                            )
                        }
                        Button(onClick = {
                            // Parse each line and add as new items or update existing ones
                            val lines = notesText.split("\n").filter { it.trim().isNotEmpty() }
                            if (lines.isNotEmpty()) {
                                val updatedGroceries = groceries.toMutableList()
                                
                                lines.forEach { line ->
                                    val itemName = line.trim()
                                    val existingItemIndex = updatedGroceries.indexOfFirst { it.name == itemName }
                                    
                                    if (existingItemIndex != -1) {
                                        // Item exists, just set inShoppingList to true
                                        updatedGroceries[existingItemIndex] = updatedGroceries[existingItemIndex].copy(
                                            inShoppingList = true
                                        )
                                    } else {
                                        // Item doesn't exist, create new item
                                        val newItem = GroceryWithDate(
                                            name = itemName,
                                            customCategoryId = 1, // Default to "אחר"
                                            expirationDate = null,
                                            lastTimeBoughtDays = null,
                                            averageBuyingDays = null,
                                            buyEvents = emptyList(),
                                            inShoppingList = true
                                        )
                                        updatedGroceries.add(newItem)
                                    }
                                }
                                
                                onUpdateGroceries(updatedGroceries)
                            }
                            notesText = ""
                            showNotesDialog = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "הוסף",
                                tint = Color.White
                            )
                        }
                    }
                },
                title = { Text("הוסף רשימה") },
                text = {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("הוסף פריטים. כל פריט בשורה נפרדת.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 10
                    )
                }
            )
        }
        
        // Categories list dialog
        if (showCategoriesList) {
            AlertDialog(
                onDismissRequest = { showCategoriesList = false },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Button(onClick = { showCategoriesList = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = Color.White
                            )
                        }
                        Button(
                            onClick = { 
                                newCategoryName = ""
                                showCreateCategoryDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "צור קטגוריה חדשה",
                                tint = Color.White
                            )
                        }
                    }
                },
                title = { Text("רשימת קטגוריות") },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(customCategories.sortedBy { it.viewOrder }) { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.name,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        // Move category up
                                        val currentIndex = customCategories.indexOf(category)
                                        if (currentIndex > 0) {
                                            val updatedCategories = customCategories.toMutableList()
                                            val temp = updatedCategories[currentIndex]
                                            updatedCategories[currentIndex] = updatedCategories[currentIndex - 1]
                                            updatedCategories[currentIndex - 1] = temp
                                            // Update viewOrder values
                                            updatedCategories.forEachIndexed { index, cat ->
                                                updatedCategories[index] = cat.copy(viewOrder = index + 1)
                                            }
                                            onUpdateCategories(updatedCategories)
                                        }
                                    },
                                    enabled = customCategories.indexOf(category) > 0
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "העלה",
                                        tint = if (customCategories.indexOf(category) > 0) Color.Black else Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        // Move category down
                                        val currentIndex = customCategories.indexOf(category)
                                        if (currentIndex < customCategories.size - 1) {
                                            val updatedCategories = customCategories.toMutableList()
                                            val temp = updatedCategories[currentIndex]
                                            updatedCategories[currentIndex] = updatedCategories[currentIndex + 1]
                                            updatedCategories[currentIndex + 1] = temp
                                            // Update viewOrder values
                                            updatedCategories.forEachIndexed { index, cat ->
                                                updatedCategories[index] = cat.copy(viewOrder = index + 1)
                                            }
                                            onUpdateCategories(updatedCategories)
                                        }
                                    },
                                    enabled = customCategories.indexOf(category) < customCategories.size - 1
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "הורד",
                                        tint = if (customCategories.indexOf(category) < customCategories.size - 1) Color.Black else Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingCategory = category
                                        editingCategoryName = category.name
                                        showEditCategoryDialog = true
                                    },
                                    enabled = category.name != "אחר"
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "ערוך שם קטגוריה",
                                        tint = if (category.name == "אחר") Color.Gray else Color.Black
                                    )
                                }
                            }
                            if (category != customCategories.sortedBy { it.viewOrder }.last()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            )
        }
        
        // Edit category dialog
        if (showEditCategoryDialog && editingCategory != null) {
            AlertDialog(
                onDismissRequest = { 
                    showEditCategoryDialog = false
                    editingCategory = null
                    editingCategoryName = ""
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { 
                                showEditCategoryDialog = false
                                editingCategory = null
                                editingCategoryName = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "ביטול",
                                tint = Color.White
                            )
                        }
                        Row {
                            if (editingCategory?.name != "אחר") {
                                Button(
                                    onClick = { 
                                        categoryToDelete = editingCategory
                                        showEditCategoryDialog = false
                                        showDeleteCategoryDialog = true
                                    },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Color.Red
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "מחק",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Button(
                                onClick = {
                                    if (editingCategoryName.isNotBlank()) {
                                        val updatedCategories = customCategories.map { cat ->
                                            if (cat.id == editingCategory!!.id) {
                                                cat.copy(name = editingCategoryName)
                                            } else {
                                                cat
                                            }
                                        }
                                        onUpdateCategories(updatedCategories)
                                        showEditCategoryDialog = false
                                        editingCategory = null
                                        editingCategoryName = ""
                                    }
                                },
                                enabled = editingCategoryName.isNotBlank() && editingCategory?.name != "אחר"
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "שמור",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                },
                title = { Text("ערוך שם קטגוריה") },
                text = {
                    OutlinedTextField(
                        value = editingCategoryName,
                        onValueChange = { editingCategoryName = it },
                        label = { Text("שם הקטגוריה") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = editingCategory?.name != "אחר"
                    )
                }
            )
        }
        
        // Delete category confirmation dialog
        if (showDeleteCategoryDialog && categoryToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteCategoryDialog = false
                    categoryToDelete = null
                },
                confirmButton = {
                    Column {
                        Button(
                            onClick = {
                                // Delete all items in this category
                                val updatedGroceries = groceries.filter { it.customCategoryId != categoryToDelete!!.id }
                                onUpdateGroceries(updatedGroceries)
                                
                                // Remove the category
                                val updatedCategories = customCategories.filter { it.id != categoryToDelete!!.id }
                                onUpdateCategories(updatedCategories)
                                
                                showDeleteCategoryDialog = false
                                categoryToDelete = null
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Text("מחק את כל הפריטים", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                // Move all items to "אחר" category (ID 1)
                                val updatedGroceries = groceries.map { grocery ->
                                    if (grocery.customCategoryId == categoryToDelete!!.id) {
                                        grocery.copy(customCategoryId = 1) // Move to "אחר"
                                    } else {
                                        grocery
                                    }
                                }
                                onUpdateGroceries(updatedGroceries)
                                
                                // Remove the category
                                val updatedCategories = customCategories.filter { it.id != categoryToDelete!!.id }
                                onUpdateCategories(updatedCategories)
                                
                                showDeleteCategoryDialog = false
                                categoryToDelete = null
                            }
                        ) {
                            Text("העבר ל'אחר'")
                        }
                    }
                },
                dismissButton = {
                    Row {
                        Button(
                            onClick = { 
                                showDeleteCategoryDialog = false
                                categoryToDelete = null
                            }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                },
                title = { Text("מחק קטגוריה") },
                text = { 
                    Text("הקטגוריה '${categoryToDelete?.name}' מכילה פריטים. מה ברצונך לעשות?")
                }
            )
        }
        
        // Create category dialog
        if (showCreateCategoryDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showCreateCategoryDialog = false
                    newCategoryName = ""
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { 
                                showCreateCategoryDialog = false
                                newCategoryName = ""
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "ביטול",
                                tint = Color.White
                            )
                        }
                        Button(
                            onClick = {
                                if (newCategoryName.isNotBlank()) {
                                    // Generate a new unique ID
                                    val newId = (customCategories.maxOfOrNull { it.id } ?: 0) + 1
                                    val newViewOrder = (customCategories.maxOfOrNull { it.viewOrder } ?: 0) + 1
                                    
                                    val newCategory = CustomCategory(
                                        id = newId,
                                        name = newCategoryName,
                                        default = false,
                                        viewOrder = newViewOrder
                                    )
                                    
                                    val updatedCategories = customCategories + newCategory
                                    onUpdateCategories(updatedCategories)
                                    
                                    showCreateCategoryDialog = false
                                    newCategoryName = ""
                                }
                            },
                            enabled = newCategoryName.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "צור",
                                tint = Color.White
                            )
                        }
                    }
                },
                title = { Text("צור קטגוריה חדשה") },
                text = {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("שם הקטגוריה") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
        
        // Buy history dialog
        if (showBuyHistoryDialog && selectedGroceryForHistory != null) {
            AlertDialog(
                onDismissRequest = { 
                    showBuyHistoryDialog = false
                    selectedGroceryForHistory = null
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showBuyHistoryDialog = false
                            selectedGroceryForHistory = null
                        }
                    ) {
                        Text("סגור")
                    }
                },
                title = { Text("היסטוריית קניות - ${selectedGroceryForHistory?.name}") },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        items(selectedGroceryForHistory?.buyEvents?.sorted()?.reversed() ?: emptyList()) { buyDate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = buyDate.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${ChronoUnit.DAYS.between(buyDate, LocalDate.now())} ימים",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                            if (buyDate != selectedGroceryForHistory?.buyEvents?.sorted()?.reversed()?.last()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            )
        }
        
        // Add to shopping list confirmation dialog
        if (showAddToShoppingListConfirm) {
            AlertDialog(
                onDismissRequest = { 
                    showAddToShoppingListConfirm = false
                    showDialog = false
                    name = ""
                    selectedCustomCategoryId = 1
                    expirationDate = null
                    inShoppingList = false
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { 
                                // Add item without shopping list
                                val updatedGroceries = groceries + GroceryWithDate(
                                    name = name,
                                    customCategoryId = selectedCustomCategoryId,
                                    expirationDate = expirationDate,
                                    inShoppingList = false
                                )
                                onUpdateGroceries(updatedGroceries)
                                showAddToShoppingListConfirm = false
                                showDialog = false
                                name = ""
                                selectedCustomCategoryId = 1
                                expirationDate = null
                                inShoppingList = false
                            }
                        ) {
                            Text("לא")
                        }
                        Button(
                            onClick = { 
                                // Add item with shopping list
                                val updatedGroceries = groceries + GroceryWithDate(
                                    name = name,
                                    customCategoryId = selectedCustomCategoryId,
                                    expirationDate = expirationDate,
                                    inShoppingList = true
                                )
                                onUpdateGroceries(updatedGroceries)
                                showAddToShoppingListConfirm = false
                                showDialog = false
                                name = ""
                                selectedCustomCategoryId = 1
                                expirationDate = null
                                inShoppingList = false
                            }
                        ) {
                            Text("כן")
                        }
                    }
                },
                title = { Text("הוסף לרשימת קניות") },
                text = { Text("האם ברצונך להוסיף את '${name}' לרשימת הקניות?") }
            )
        }
        
        // Alert notification dialog
        if (showAlertNotification) {
            AlertDialog(
                onDismissRequest = { showAlertNotification = false },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { showAlertNotification = false }
                        ) {
                            Text("סגור")
                        }
                        Button(
                            onClick = { 
                                showExpiringOnly = true
                                showAlertNotification = false
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            )
                        ) {
                            Text("הצג פריטים", color = Color.White)
                        }
                    }
                },
                title = { Text("פריטים שדורשים תשומת לב") },
                text = { 
                    Text("יש פריטים שפג תוקפם, עומדים לפוג, או עבר ממוצע הקנייה שלהם. האם ברצונך לראות אותם?")
                }
            )
        }
        
        // Language selection dialog
        if (showLanguageSelection) {
            AlertDialog(
                onDismissRequest = { showLanguageSelection = false },
                confirmButton = {
                    Button(
                        onClick = { showLanguageSelection = false }
                    ) {
                        Text("סגור")
                    }
                },
                title = { Text(localizedString("choose_language", selectedLanguage)) },
                text = {
                    Column {
                        Button(
                            onClick = { 
                                onLanguageChange("iw")
                                showLanguageSelection = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (selectedLanguage == "iw") Color(0xFF4CAF50) else Color.Gray
                            )
                        ) {
                            Text("עברית", color = Color.White, fontWeight = if (selectedLanguage == "iw") FontWeight.Bold else FontWeight.Normal)
                        }
                        Button(
                            onClick = { 
                                onLanguageChange("en")
                                showLanguageSelection = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (selectedLanguage == "en") Color(0xFF4CAF50) else Color.Gray
                            )
                        ) {
                            Text("English", color = Color.White, fontWeight = if (selectedLanguage == "en") FontWeight.Bold else FontWeight.Normal)
                        }
                        Button(
                            onClick = { 
                                onLanguageChange("ru")
                                showLanguageSelection = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (selectedLanguage == "ru") Color(0xFF4CAF50) else Color.Gray
                            )
                        ) {
                            Text("Русский", color = Color.White, fontWeight = if (selectedLanguage == "ru") FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    shoppingList: List<GroceryWithDate>,
    groceries: List<GroceryWithDate>,
    onUpdateGroceries: (List<GroceryWithDate>) -> Unit,
    onRemove: (GroceryWithDate) -> Unit,
    onBuy: (GroceryWithDate) -> Unit,
    orderedCategories: List<CustomCategory>,
    customCategories: List<CustomCategory>
) {
    val context = LocalContext.current
    val categoryExpansion = remember { mutableStateMapOf<Int, Boolean>() }
    orderedCategories.forEach { cat ->
        if (categoryExpansion[cat.id] == null) categoryExpansion[cat.id] = true
    }

    // Edit state variables
    var showEditDialog by remember { mutableStateOf(false) }
    var editGrocery by remember { mutableStateOf<GroceryWithDate?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Edit dialog fields
    var name by remember { mutableStateOf("") }
    var selectedCustomCategoryId by remember { mutableStateOf(1) }
    var expirationDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) } // For category dropdown
    
    // Shopping workflow state
    var boughtItems by remember { mutableStateOf<List<GroceryWithDate>>(emptyList()) }
    var showDoneShoppingConfirm by remember { mutableStateOf(false) }
    
    // Load bought items from DataStore on first composition
    LaunchedEffect(Unit) {
        val loaded = context.boughtItemsDataStore.data.first().map { it.withLocalDate() }
        boughtItems = loaded
    }
    
    // Save bought items to DataStore whenever they change
    LaunchedEffect(boughtItems) {
        context.boughtItemsDataStore.updateData { boughtItems.map { it.toSerializable() } }
    }

    fun openEditDialog(grocery: GroceryWithDate) {
        name = grocery.name
        selectedCustomCategoryId = grocery.customCategoryId
        expirationDate = grocery.expirationDate
        editGrocery = grocery
        showEditDialog = true
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        orderedCategories.forEach { category ->
            val itemsInCategory = shoppingList.filter { it.customCategoryId == category.id }
            if (itemsInCategory.isNotEmpty()) {
                item(key = category.id) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryExpansion[category.id] = !(categoryExpansion[category.id] ?: true) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Badge { Text(itemsInCategory.size.toString()) }
                                Text(
                                    text = if (categoryExpansion[category.id] == true) "▲" else "▼",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Divider()
                            if (categoryExpansion[category.id] == true) {
                                itemsInCategory.forEach { grocery ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                            .background(
                                                Color(0xFFF8F9FA), // Very light gray background
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = grocery.name,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { onRemove(grocery) },
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "הסר"
                                            )
                                        }
                                        IconButton(
                                            onClick = { openEditDialog(grocery) },
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = stringResource(R.string.edit)
                                            )
                                        }
                                        IconButton(
                                            onClick = { 
                                                // Move item to bought list instead of buying immediately
                                                boughtItems = boughtItems + grocery
                                                onRemove(grocery) // Remove from shopping list
                                            },
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = stringResource(R.string.add_to_bought)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Separator line
        if (shoppingList.isNotEmpty() || boughtItems.isNotEmpty()) {
            item {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    thickness = 2.dp,
                    color = Color.Gray
                )
            }
        }
        
        // Bought items section
        if (boughtItems.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.bought_items),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            boughtItems.forEach { boughtItem ->
                item(key = "bought_${boughtItem.name}_${boughtItem.customCategoryId}") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(
                                Color(0xFFE8F5E8), // Light green background for bought items
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.bought),
                            tint = Color.Green,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = boughtItem.name,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Medium
                        )
                        val category = customCategories.find { it.id == boughtItem.customCategoryId }
                        Text(
                            text = category?.name ?: "",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        IconButton(
                            onClick = { 
                                // Return item to shopping list
                                boughtItems = boughtItems.filter { it != boughtItem }
                                // Add back to shopping list
                                val updatedGroceries = groceries.map {
                                    if (it.name == boughtItem.name && it.customCategoryId == boughtItem.customCategoryId) {
                                        it.copy(inShoppingList = true)
                                    } else {
                                        it
                                    }
                                }
                                onUpdateGroceries(updatedGroceries)
                            },
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "החזר לרשימת קניות",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }
            
            // Done shopping button
            item {
                Button(
                    onClick = { showDoneShoppingConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = stringResource(R.string.finish_shopping),
                        tint = Color.White,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.finish_shopping_with_count, boughtItems.size),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Edit dialog
    if (showEditDialog && editGrocery != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Button(onClick = { showEditDialog = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "ביטול",
                            tint = Color.White
                        )
                    }
                    Button(onClick = {
                        val updated = editGrocery!!.copy(
                            name = name,
                            customCategoryId = selectedCustomCategoryId,
                            expirationDate = expirationDate
                        )
                        val updatedGroceries = groceries.map {
                            if (it.name == editGrocery!!.name && it.customCategoryId == editGrocery!!.customCategoryId) updated else it
                        }
                        onUpdateGroceries(updatedGroceries)
                        showEditDialog = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "שמור",
                            tint = Color.White
                        )
                    }
                }
            },
            title = { Text("ערוך מצרך") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.item_name)) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Category dropdown
                    Box {
                        val selectedCategory = customCategories.find { it.id == selectedCustomCategoryId }
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedCategory?.name ?: stringResource(R.string.choose_category))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            customCategories.sortedBy { it.viewOrder }.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCustomCategoryId = cat.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Expiration date picker
                    Button(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                                            ) {
                            Text(if (expirationDate != null) expirationDate.toString() else stringResource(R.string.expiration_date))
                        }
                    if (showDatePicker) {
                        val datePickerState = rememberDatePickerState()
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                Button(onClick = {
                                    val millis = datePickerState.selectedDateMillis
                                    expirationDate = millis?.let {
                                        LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                                    }
                                    showDatePicker = false
                                }) { Text(stringResource(R.string.save)) }
                            },
                            dismissButton = {
                                Button(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }
                }
            }
        )
    }
    
    // Done shopping confirmation dialog
    if (showDoneShoppingConfirm) {
        AlertDialog(
            onDismissRequest = { showDoneShoppingConfirm = false },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { showDoneShoppingConfirm = false }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = { 
                            // Execute buy process for all bought items
                            boughtItems.forEach { boughtItem ->
                                onBuy(boughtItem)
                            }
                            // Clear bought items list (DataStore will be updated automatically via LaunchedEffect)
                            boughtItems = emptyList()
                            showDoneShoppingConfirm = false
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text(stringResource(R.string.save), color = Color.White)
                    }
                }
            },
                            title = { Text(stringResource(R.string.confirm_finish_shopping)) },
            text = { 
                Text(stringResource(R.string.confirm_finish_shopping_message, boughtItems.size))
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SuperCartAppPreview() {
    SuperCartTheme {
        SuperCartApp()
    }
}

val Context.groceryDataStore: DataStore<List<Grocery>> by dataStore(
    fileName = "groceries.json",
    serializer = GroceryListSerializer
)

object GroceryListSerializer : Serializer<List<Grocery>> {
    override val defaultValue: List<Grocery> = emptyList()
    override suspend fun readFrom(input: InputStream): List<Grocery> =
        runCatching {
            Json.decodeFromString(ListSerializer(Grocery.serializer()), input.readBytes().decodeToString())
        }.getOrDefault(emptyList())
    override suspend fun writeTo(t: List<Grocery>, output: OutputStream) {
        output.write(Json.encodeToString(ListSerializer(Grocery.serializer()), t).encodeToByteArray())
    }
}

val Context.categoryOrderDataStore by preferencesDataStore(name = "category_order_prefs")
val CATEGORY_ORDER_KEY = stringPreferencesKey("category_order")

// Bought items DataStore
val Context.boughtItemsDataStore: DataStore<List<Grocery>> by dataStore(
    fileName = "bought_items.json",
    serializer = GroceryListSerializer
)

// Language selection DataStore
val Context.languageDataStore by preferencesDataStore(name = "language_prefs")
val LANGUAGE_KEY = stringPreferencesKey("selected_language")

// Custom categories DataStore
val Context.customCategoriesDataStore: DataStore<List<CustomCategory>> by dataStore(
    fileName = "custom_categories.json",
    serializer = CustomCategoryListSerializer
)

object CustomCategoryListSerializer : Serializer<List<CustomCategory>> {
    override val defaultValue: List<CustomCategory> = emptyList()
    override suspend fun readFrom(input: InputStream): List<CustomCategory> =
        runCatching {
            Json.decodeFromString(ListSerializer(CustomCategory.serializer()), input.readBytes().decodeToString())
        }.getOrDefault(emptyList())
    override suspend fun writeTo(t: List<CustomCategory>, output: OutputStream) {
        output.write(Json.encodeToString(ListSerializer(CustomCategory.serializer()), t).encodeToByteArray())
    }
}

// Function to initialize default custom categories
suspend fun Context.initializeDefaultCustomCategories() {
    val defaultCategories = listOf(
        CustomCategory(id = 1, name = "אחר", default = true, viewOrder = 1),
        CustomCategory(id = 2, name = "פירות", default = true, viewOrder = 2),
        CustomCategory(id = 3, name = "ירקות", default = true, viewOrder = 3),
        CustomCategory(id = 4, name = "מאפים ולחמים", default = true, viewOrder = 4),
        CustomCategory(id = 5, name = "חטיפים ומתוקים", default = true, viewOrder = 5),
        CustomCategory(id = 6, name = "דגנים וקטניות", default = true, viewOrder = 6),
        CustomCategory(id = 7, name = "שימורים", default = true, viewOrder = 7),
        CustomCategory(id = 8, name = "חד פעמי", default = true, viewOrder = 8),
        CustomCategory(id = 9, name = "מוצרי נקיון", default = true, viewOrder = 9),
        CustomCategory(id = 10, name = "מוצרים לתינוקות", default = true, viewOrder = 10),
        CustomCategory(id = 11, name = "מזון יבש", default = true, viewOrder = 11),
        CustomCategory(id = 12, name = "תבלינים ורטבים", default = true, viewOrder = 12),
        CustomCategory(id = 13, name = "מוצרי טואלטיקה", default = true, viewOrder = 13),
        CustomCategory(id = 14, name = "משקאות", default = true, viewOrder = 14),
        CustomCategory(id = 15, name = "קפואים", default = true, viewOrder = 15),
        CustomCategory(id = 16, name = "מוצרי חלב", default = true, viewOrder = 16),
        CustomCategory(id = 17, name = "בשר ודגים", default = true, viewOrder = 17),
        CustomCategory(id = 18, name = "מוצרים לבית", default = true, viewOrder = 18)
    )
    
    // Check if "אחר" category with id = 1 already exists
    val existingCategories = customCategoriesDataStore.data.first()
    val hasAcherCategory = existingCategories.any { it.id == 1 && it.name == "אחר" }
    
    if (!hasAcherCategory) {
        // Initialize with default categories
        customCategoriesDataStore.updateData { defaultCategories }
    }
}

// Migration function to convert old enum-based groceries to custom categories
suspend fun Context.migrateGroceriesToCustomCategories() {
    val groceries = groceryDataStore.data.first()
    val customCategories = customCategoriesDataStore.data.first()
    
    // Simple migration: ensure all groceries have a valid customCategoryId
    // If any grocery has customCategoryId = null or 0, set it to 1 (אחר)
    val needsMigration = groceries.any { it.customCategoryId == 0 }
    
    if (needsMigration) {
        val migratedGroceries = groceries.map { grocery ->
            // Ensure customCategoryId is valid (default to 1 if invalid)
            val validCategoryId = if (grocery.customCategoryId <= 0) 1 else grocery.customCategoryId
            Grocery(
                name = grocery.name,
                customCategoryId = validCategoryId,
                expirationDate = grocery.expirationDate,
                lastTimeBoughtDays = grocery.lastTimeBoughtDays,
                averageBuyingDays = grocery.averageBuyingDays,
                buyEvents = grocery.buyEvents,
                inShoppingList = grocery.inShoppingList
            )
        }
        
        // Save migrated groceries
        groceryDataStore.updateData { migratedGroceries }
    }
}

