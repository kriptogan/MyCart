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
import androidx.compose.foundation.layout.heightIn
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope

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
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        TabItem("בית", Icons.Default.Home),
        TabItem("רשימת קניות", Icons.Default.ShoppingCart)
    )
    var groceries by remember { mutableStateOf(listOf<GroceryWithDate>()) }
    val context = LocalContext.current
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

    // Helper function to get shopping list items by filtering inShoppingList = true
    val shoppingListItems = remember(groceries) {
        groceries.filter { it.inShoppingList }
    }

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
                                if (tabItem.title == "רשימת קניות" && shoppingListItems.isNotEmpty()) {
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
                                    it.copy(inShoppingList = true)
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
                        scope = scope
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
    scope: CoroutineScope
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

    fun openEditDialog(index: Int, grocery: GroceryWithDate) {
        name = grocery.name
        selectedCustomCategoryId = grocery.customCategoryId
        expirationDate = grocery.expirationDate
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
                            contentDescription = "הוסף מצרך",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(30.dp))
                    IconButton(
                        onClick = { showNotesDialog = true },
                        modifier = Modifier
                            .background(
                                color = Color(0xFF2196F3),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .size(48.dp)
                            .padding(start = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "הערות",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = { showCategoriesList = true },
                        modifier = Modifier
                            .background(
                                color = Color(0xFF9C27B0),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "רשימת קטגוריות",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
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
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("חפש מצרך...") },
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
                                                    contentDescription = "ערוך"
                                                )
                                            }
                                            IconButton(
                                                onClick = { onAddToShoppingList(indexedGrocery.value) },
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ShoppingCart,
                                                    contentDescription = "הוסף לרשימת קניות",
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
                    Button(onClick = {
                        if (name.isNotBlank()) {
                            if (isEditMode && editIndex >= 0) {
                                val updatedGroceries = groceries.toMutableList().also {
                                    it[editIndex] = it[editIndex].copy(
                                        name = name,
                                        customCategoryId = selectedCustomCategoryId,
                                        expirationDate = expirationDate
                                    )
                                }
                                onUpdateGroceries(updatedGroceries)
                            } else {
                                val updatedGroceries = groceries + GroceryWithDate(
                                    name = name,
                                    customCategoryId = selectedCustomCategoryId,
                                    expirationDate = expirationDate
                                )
                                onUpdateGroceries(updatedGroceries)
                            }
                            name = ""
                            selectedCustomCategoryId = 1 // Default to "אחר"
                            expirationDate = null
                            showDialog = false
                        }
                    }) {
                        Text(if (isEditMode) "שמור" else "הוסף")
                    }
                },
                dismissButton = {
                    Row {
                        Button(onClick = { showDialog = false }) {
                            Text("ביטול")
                        }
                        if (isEditMode) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { showDeleteConfirm = true }) {
                                Text("מחק", color = Color.Red)
                            }
                        }
                    }
                },
                title = { Text(if (isEditMode) "ערוך מצרך" else "הוסף מצרך") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("שם המצרך") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Category dropdown
                        Box {
                            val selectedCategory = customCategories.find { it.id == selectedCustomCategoryId }
                            Button(onClick = { expanded = true }) {
                                Text(selectedCategory?.name ?: "בחר קטגוריה")
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
                        Button(onClick = { showDatePicker = true }) {
                            Text(if (expirationDate != null) expirationDate.toString() else "בחר תאריך תפוגה (אופציונלי)")
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
                                    }) { Text("אישור") }
                                },
                                dismissButton = {
                                    Button(onClick = { showDatePicker = false }) { Text("ביטול") }
                                }
                            ) {
                                DatePicker(state = datePickerState)
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
                    Button(onClick = {
                        val updatedGroceries = groceries.toMutableList().also { it.removeAt(editIndex) }
                        onUpdateGroceries(updatedGroceries)
                        showDeleteConfirm = false
                        showDialog = false
                    }) {
                        Text("מחק", color = Color.Red)
                    }
                },
                dismissButton = {
                    Button(onClick = { showDeleteConfirm = false }) {
                        Text("ביטול")
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
                        Text("הוסף")
                    }
                },
                dismissButton = {
                    Button(onClick = { 
                        notesText = ""
                        showNotesDialog = false 
                    }) {
                        Text("סגור")
                    }
                },
                title = { Text("הערות") },
                text = {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("כתוב הערות...") },
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
                    Button(onClick = { showCategoriesList = false }) {
                        Text("סגור")
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
                                    fontWeight = if (category.default) FontWeight.Bold else FontWeight.Normal
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
                            }
                            if (category != customCategories.sortedBy { it.viewOrder }.last()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
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
                                                contentDescription = "ערוך"
                                            )
                                        }
                                        IconButton(
                                            onClick = { onBuy(grocery) },
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "נרכש"
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

    // Edit dialog
    if (showEditDialog && editGrocery != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            confirmButton = {
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
                    Text("שמור")
                }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = false }) {
                    Text("ביטול")
                }
            },
            title = { Text("ערוך מצרך") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("שם המצרך") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Category dropdown
                    Box {
                        val selectedCategory = customCategories.find { it.id == selectedCustomCategoryId }
                        Button(onClick = { expanded = true }) {
                            Text(selectedCategory?.name ?: "בחר קטגוריה")
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
                    Button(onClick = { showDatePicker = true }) {
                        Text(if (expirationDate != null) expirationDate.toString() else "בחר תאריך תפוגה (אופציונלי)")
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
                                }) { Text("אישור") }
                            },
                            dismissButton = {
                                Button(onClick = { showDatePicker = false }) { Text("ביטול") }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }
                }
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

