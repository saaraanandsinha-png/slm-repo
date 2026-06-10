package com.example.saaraapp.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.saaraapp.NotificationViewModel
import com.example.saaraapp.SettingsViewModel
import com.example.saaraapp.ui.components.PreferenceItem
import com.example.saaraapp.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current

    val darkThemePref by settingsViewModel.darkTheme.collectAsState()
    val amoledMode    by settingsViewModel.amoledMode.collectAsState()
    val systemDark    = isSystemInDarkTheme()
    val isDark        = darkThemePref ?: systemDark

    // ── Notification access — recheck on every resume ──────────────────────
    fun checkAccess(): Boolean {
        val listeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return listeners?.contains(context.packageName) == true
    }

    var notificationAccessGranted by remember { mutableStateOf(checkAccess()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationAccessGranted = checkAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Clear-all confirmation dialog ──────────────────────────────────────
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all reminders?") },
            text  = { Text("This will permanently delete every saved reminder. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    notificationViewModel.clearAll()
                    showClearDialog = false
                }) {
                    Text("Clear all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── UI ─────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {

            // ── Appearance ─────────────────────────────────────────────────
            SectionHeader("Appearance")

            PreferenceItem(
                title    = "Dark Theme",
                subtitle = if (darkThemePref == null) "Following system setting"
                           else if (isDark) "Enabled" else "Disabled",
                trailing = {
                    Switch(
                        checked         = isDark,
                        onCheckedChange = { settingsViewModel.setDarkTheme(it) }
                    )
                }
            )

            PreferenceItem(
                title    = "AMOLED Mode",
                subtitle = if (!isDark) "Only available in dark mode"
                           else if (amoledMode) "Pure black background active" else "Off",
                trailing = {
                    Switch(
                        checked         = amoledMode,
                        onCheckedChange = { settingsViewModel.setAmoledMode(it) },
                        enabled         = isDark
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Notifications ──────────────────────────────────────────────
            SectionHeader("Notifications")

            PreferenceItem(
                title    = "Notification Access",
                subtitle = if (notificationAccessGranted)
                               "Granted — reading WhatsApp notifications"
                           else
                               "Not granted — tap to enable in system settings",
                onClick  = {
                    if (!notificationAccessGranted) {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        )
                    }
                },
                trailing = {
                    Icon(
                        imageVector  = if (notificationAccessGranted) Icons.Default.CheckCircle
                                       else Icons.Default.Warning,
                        contentDescription = null,
                        tint         = if (notificationAccessGranted)
                                           MaterialTheme.colorScheme.primary
                                       else
                                           MaterialTheme.colorScheme.error
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ── Data ───────────────────────────────────────────────────────
            SectionHeader("Data")

            PreferenceItem(
                title    = "Clear All Reminders",
                subtitle = "Permanently delete all saved reminders",
                onClick  = { showClearDialog = true }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
