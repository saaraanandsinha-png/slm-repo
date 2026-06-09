package com.example.saaraapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.saaraapp.ui.navigation.BottomNavBar
import com.example.saaraapp.ui.navigation.NavGraph
import com.example.saaraapp.ui.navigation.Screen
import com.example.saaraapp.ui.theme.SaaraAppTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaaraAppTheme {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val current = backStack?.destination?.route

                // Hide top bar on settings screen
                val showTopBar = current?.contains("Settings") != true

                Scaffold(
                    modifier  = Modifier.fillMaxSize(),
                    topBar    = {
                        if (showTopBar) {
                            TopAppBar(
                                title = { Text("📬 Reminders") },
                                actions = {
                                    IconButton(onClick = {
                                        navController.navigate(Screen.Settings)
                                    }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        if (showTopBar) BottomNavBar(navController)
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        modifier      = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
