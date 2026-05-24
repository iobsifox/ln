package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.BgDark
import com.example.ui.theme.BorderDark
import com.example.ui.theme.BlueNeon
import com.example.ui.theme.LastNightTheme
import com.example.ui.theme.SurfaceDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Instantiate our View Model simply
        val viewModel = MainViewModel(applicationContext)

        setContent {
            LastNightTheme {
                MainAppContainer(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: MainViewModel) {
    var activeTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "LAST NIGHT",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(imageVector = if (activeTab == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BlueNeon,
                        selectedTextColor = BlueNeon,
                        indicatorColor = BorderDark,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(imageVector = if (activeTab == 1) Icons.Filled.Dns else Icons.Outlined.Dns, contentDescription = "DNS & Modes") },
                    label = { Text("DNS/Modes", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BlueNeon,
                        selectedTextColor = BlueNeon,
                        indicatorColor = BorderDark,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(imageVector = if (activeTab == 2) Icons.Filled.Share else Icons.Outlined.Share, contentDescription = "Local Sharing") },
                    label = { Text("Sharing", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BlueNeon,
                        selectedTextColor = BlueNeon,
                        indicatorColor = BorderDark,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(imageVector = if (activeTab == 3) Icons.Filled.AppBlocking else Icons.Outlined.AppBlocking, contentDescription = "App Routing") },
                    label = { Text("Routing", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BlueNeon,
                        selectedTextColor = BlueNeon,
                        indicatorColor = BorderDark,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )

                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(imageVector = if (activeTab == 4) Icons.Filled.Terminal else Icons.Outlined.Terminal, contentDescription = "Daemon Cores") },
                    label = { Text("Cores", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BlueNeon,
                        selectedTextColor = BlueNeon,
                        indicatorColor = BorderDark,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgDark)
        ) {
            when (activeTab) {
                0 -> HomeScreen(viewModel)
                
                // Group 2: Tab-switched Modes vs DNS Settings
                1 -> {
                    var modeSubTab by remember { mutableStateOf(0) }
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = modeSubTab,
                            containerColor = SurfaceDark,
                            contentColor = Color.White
                        ) {
                            Tab(
                                selected = modeSubTab == 0,
                                onClick = { modeSubTab = 0 },
                                text = { Text("MODES & PROFILES", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = modeSubTab == 1,
                                onClick = { modeSubTab = 1 },
                                text = { Text("DOH SETTINGS", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            if (modeSubTab == 0) {
                                ModesScreen(viewModel)
                            } else {
                                DnsSettingsScreen(viewModel)
                            }
                        }
                    }
                }
                
                2 -> LocalSharingScreen(viewModel)
                
                3 -> VpnAppsAccessScreen(viewModel)
                
                // Group 5: Tab-switched Cores configuration vs live Logs terminal console
                4 -> {
                    var coreSubTab by remember { mutableStateOf(0) }
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = coreSubTab,
                            containerColor = SurfaceDark,
                            contentColor = Color.White
                        ) {
                            Tab(
                                selected = coreSubTab == 0,
                                onClick = { coreSubTab = 0 },
                                text = { Text("DAEMON ADAPTERS", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = coreSubTab == 1,
                                onClick = { coreSubTab = 1 },
                                text = { Text("TERMINAL TRACES", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            if (coreSubTab == 0) {
                                CoresScreen(viewModel)
                            } else {
                                LogsScreen(viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
