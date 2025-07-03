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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Custom Status Bar at the top
        CustomStatusBar()
        
        // Main content
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
                            icon = { Icon(imageVector = tabItem.icon, contentDescription = tabItem.title) },
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
                    0 -> HomeScreen()
                    1 -> ShoppingListScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var groceries by remember { mutableStateOf(listOf<Grocery>()) }
    var showDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf(-1) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Fields for new/edit grocery
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(GroceryCategory.פירות) }
    var expirationDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    fun openEditDialog(index: Int, grocery: Grocery) {
        name = grocery.name
        selectedCategory = grocery.category
        expirationDate = grocery.expirationDate
        editIndex = index
        isEditMode = true
        showDialog = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            itemsIndexed(groceries) { index: Int, grocery: Grocery ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${grocery.name} - ${grocery.category.displayName}", modifier = Modifier.weight(1f))
                    Button(onClick = { openEditDialog(index, grocery) }, modifier = Modifier.padding(start = 8.dp)) {
                        Text("ערוך")
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = {
                name = ""
                selectedCategory = GroceryCategory.פירות
                expirationDate = null
                isEditMode = false
                showDialog = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Text("+")
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    Button(onClick = {
                        if (name.isNotBlank()) {
                            if (isEditMode && editIndex >= 0) {
                                groceries = groceries.toMutableList().also {
                                    it[editIndex] = it[editIndex].copy(
                                        name = name,
                                        category = selectedCategory,
                                        expirationDate = expirationDate
                                    )
                                }
                            } else {
                                groceries = groceries + Grocery(
                                    name = name,
                                    category = selectedCategory,
                                    expirationDate = expirationDate
                                )
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
                        groceries = groceries.toMutableList().also { it.removeAt(editIndex) }
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
    }
}

@Composable
fun ShoppingListScreen() {
    Text(text = "זהו מסך רשימת הקניות")
}

@Preview(showBackground = true)
@Composable
fun SuperCartAppPreview() {
    SuperCartTheme {
        SuperCartApp()
    }
}