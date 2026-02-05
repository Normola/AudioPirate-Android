package uk.co.undergroundbunker.audiopirate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uk.co.undergroundbunker.audiopirate.ui.screens.DownloadsScreen
import uk.co.undergroundbunker.audiopirate.ui.screens.HomeScreen
import uk.co.undergroundbunker.audiopirate.ui.screens.SettingsScreen
import uk.co.undergroundbunker.audiopirate.ui.screens.StreamScreen
import uk.co.undergroundbunker.audiopirate.ui.theme.AudioPirateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioPirateTheme {
                AudioPirateApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPirateApp() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                listOf(
                    NavigationItem("home", "Home", Icons.Filled.Home),
                    NavigationItem("stream", "Stream", Icons.Filled.Wifi),
                    NavigationItem("downloads", "Recordings", Icons.Filled.Folder),
                    NavigationItem("settings", "Settings", Icons.Filled.Settings)
                ).forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(navController = navController) }
            composable("stream") { StreamScreen() }
            composable("stream/{serverUrl}/{password}/{autoConnect}") { backStackEntry ->
                val serverUrl = backStackEntry.arguments?.getString("serverUrl") ?: ""
                val password = backStackEntry.arguments?.getString("password") ?: "audiopirate"
                val autoConnect = backStackEntry.arguments?.getString("autoConnect")?.toBoolean() ?: false
                StreamScreen(
                    initialUrl = java.net.URLDecoder.decode(serverUrl, "UTF-8"),
                    initialPassword = java.net.URLDecoder.decode(password, "UTF-8"),
                    autoConnect = autoConnect
                )
            }
            composable("downloads") { DownloadsScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
