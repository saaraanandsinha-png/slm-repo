package com.example.saaraapp

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.saaraapp.ui.navigation.BottomNavBar
import com.example.saaraapp.ui.navigation.NavGraph
import com.example.saaraapp.ui.navigation.Screen
import com.example.saaraapp.ui.theme.SaaraAppTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        enableEdgeToEdge()
        // Force the NotificationListenerService to rebind — fixes the stale
        // binding that happens after a reinstall even when permission is granted
        NotificationListenerService.requestRebind(
            ComponentName(this, WhatsAppNotificationService::class.java)
        )
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val darkThemePref by settingsViewModel.darkTheme.collectAsState()
            val amoledMode    by settingsViewModel.amoledMode.collectAsState()
            val systemDark    = isSystemInDarkTheme()
            val darkTheme     = darkThemePref ?: systemDark

            SaaraAppTheme(darkTheme = darkTheme, amoledMode = amoledMode) {
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
