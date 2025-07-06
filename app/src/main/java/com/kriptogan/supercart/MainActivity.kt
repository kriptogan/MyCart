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
import com.kriptogan.supercart.GroceryCategory
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
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
            .background(Color.Black.copy(alpha = 0.8f)),
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
                        .background(Color.White, shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
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
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, tabItem ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                if (tabItem.title == "רשימת קניות" && shoppingListItems.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text(shoppingListItems.size.toString()) } }) {
                                        Icon(imageVector = tabItem.icon, contentDescription = tabItem.title)
                                    }
                                } else {
                                    Icon(imageVector = tabItem.icon, contentDescription = tabItem.title)
                                }
                            },
                            label = { Text(text = tabItem.title) }
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
                                if (it.name == grocery.name && it.category == grocery.category) {
                                    it.copy(inShoppingList = true)
                                } else {
                                    it
                                }
                            }
                        }
                    )
                    1 -> ShoppingListScreen(
                        shoppingList = shoppingListItems,
                        groceries = groceries,
                        onUpdateGroceries = { groceries = it },
                        onRemove = { grocery ->
                            groceries = groceries.map {
                                if (it.name == grocery.name && it.category == grocery.category) {
                                    it.copy(inShoppingList = false)
                                } else {
                                    it
                                }
                            }
                        },
                        onBuy = { grocery ->
                            groceries = groceries.map {
                                if (it.name == grocery.name && it.category == grocery.category) {
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
                        }
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
    onAddToShoppingList: (GroceryWithDate) -> Unit
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

    // Helper to check if a grocery is expired or expiring soon
    fun isExpiringOrExpired(grocery: GroceryWithDate): Boolean {
        val exp = grocery.expirationDate ?: return false
        return try {
            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), exp)
            daysUntil <= 1L
        } catch (e: Exception) { false }
    }
    val hasExpiring = groceries.any { it.expirationDate != null && isExpiringOrExpired(it) }

    // Fields for new/edit grocery
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(GroceryCategory.פירות) }
    var expirationDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    // State for expanded/collapsed categories
    val categoryExpansion = remember { mutableStateMapOf<GroceryCategory, Boolean>() }
    GroceryCategory.values().forEach { cat ->
        if (categoryExpansion[cat] == null) categoryExpansion[cat] = true
    }

    fun openEditDialog(index: Int, grocery: GroceryWithDate) {
        name = grocery.name
        selectedCategory = grocery.category
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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            name = ""
                            selectedCategory = GroceryCategory.פירות
                            expirationDate = null
                            isEditMode = false
                            showDialog = true
                        },
                        modifier = Modifier
                    ) {
                        Text("+")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { showNotesDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("הערות")
                    }
                    IconButton(
                        onClick = { if (hasExpiring) showExpiringOnly = !showExpiringOnly },
                        enabled = hasExpiring
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "הצג רק מוצרים שפג תוקפם, עומדים לפוג, או עבר ממוצע קנייה",
                            tint = if (hasExpiring) Color(0xFFFFC107) else Color.Gray
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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
            }
            GroceryCategory.values().forEach { category ->
                val itemsInCategory = groceries.withIndex()
                    .filter { it.value.category == category && it.value.name.contains(searchQuery, ignoreCase = true) }
                    .filter { !showExpiringOnly || shouldShowInAlertFilter(it.value) }
                if (itemsInCategory.isNotEmpty()) {
                    item(key = category) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            border = CardDefaults.outlinedCardBorder(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column {
                                // Header (clickable for expand/collapse)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { categoryExpansion[category] = !(categoryExpansion[category] ?: true) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (categoryExpansion[category] == true) "▲" else "▼",
                                        fontSize = 18.sp
                                    )
                                }
                                Divider()
                                if (categoryExpansion[category] == true) {
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
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                .background(
                                                    when {
                                                        isExpiringOrExpired -> Color(0xFFFFCDD2)
                                                        shouldHighlightYellow -> Color(0xFFFFF9C4)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                        ) {
                                            Text(
                                                text = indexedGrocery.value.name,
                                                modifier = Modifier.weight(1f)
                                            )
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
                                            IconButton(
                                                onClick = { openEditDialog(indexedGrocery.index, indexedGrocery.value) },
                                                modifier = Modifier.padding(start = 8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "ערוך"
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
                                        category = selectedCategory,
                                        expirationDate = expirationDate
                                    )
                                }
                                onUpdateGroceries(updatedGroceries)
                            } else {
                                val updatedGroceries = groceries + GroceryWithDate(
                                    name = name,
                                    category = selectedCategory,
                                    expirationDate = expirationDate
                                )
                                onUpdateGroceries(updatedGroceries)
                            }
                            name = ""
                            selectedCategory = GroceryCategory.פירות
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
                            Button(onClick = { expanded = true }) {
                                Text(selectedCategory.displayName)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                GroceryCategory.values().forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.displayName) },
                                        onClick = {
                                            selectedCategory = cat
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
                                        category = GroceryCategory.אחר,
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    shoppingList: List<GroceryWithDate>,
    groceries: List<GroceryWithDate>,
    onUpdateGroceries: (List<GroceryWithDate>) -> Unit,
    onRemove: (GroceryWithDate) -> Unit,
    onBuy: (GroceryWithDate) -> Unit
) {
    val categoryExpansion = remember { mutableStateMapOf<GroceryCategory, Boolean>() }
    GroceryCategory.values().forEach { cat ->
        if (categoryExpansion[cat] == null) categoryExpansion[cat] = true
    }

    // Edit state variables
    var showEditDialog by remember { mutableStateOf(false) }
    var editGrocery by remember { mutableStateOf<GroceryWithDate?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Edit dialog fields
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(GroceryCategory.פירות) }
    var expirationDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    fun openEditDialog(grocery: GroceryWithDate) {
        name = grocery.name
        selectedCategory = grocery.category
        expirationDate = grocery.expirationDate
        editGrocery = grocery
        showEditDialog = true
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        GroceryCategory.values().forEach { category ->
            val itemsInCategory = shoppingList.filter { it.category == category }
            if (itemsInCategory.isNotEmpty()) {
                item(key = category) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryExpansion[category] = !(categoryExpansion[category] ?: true) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Badge { Text(itemsInCategory.size.toString()) }
                                Text(
                                    text = if (categoryExpansion[category] == true) "▲" else "▼",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Divider()
                            if (categoryExpansion[category] == true) {
                                itemsInCategory.forEach { grocery ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = grocery.name,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { onBuy(grocery) },
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "נרכש"
                                            )
                                        }
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
                        category = selectedCategory,
                        expirationDate = expirationDate
                    )
                    val updatedGroceries = groceries.map {
                        if (it.name == editGrocery!!.name && it.category == editGrocery!!.category) updated else it
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
                        Button(onClick = { expanded = true }) {
                            Text(selectedCategory.displayName)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            GroceryCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = {
                                        selectedCategory = cat
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

