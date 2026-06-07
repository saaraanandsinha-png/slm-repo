package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapplication.ui.screens.BenchmarkScreen
import com.example.myapplication.ui.screens.ChatHistoryScreen
import com.example.myapplication.ui.screens.ChatScreen
import com.example.myapplication.ui.screens.ModelManagementScreen
import com.example.myapplication.ui.screens.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable data object Chat : Screen()
    @Serializable data object ModelManagement : Screen()
    @Serializable data object Benchmark : Screen()
    @Serializable data object ChatHistory : Screen()
    @Serializable data object Settings : Screen()
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat
    ) {
        composable<Screen.Chat> {
            ChatScreen(navController)
        }
        composable<Screen.ModelManagement> {
            ModelManagementScreen(navController)
        }
        composable<Screen.Benchmark> {
            BenchmarkScreen(navController)
        }
        composable<Screen.ChatHistory> {
            ChatHistoryScreen(navController)
        }
        composable<Screen.Settings> {
            SettingsScreen(navController)
        }
    }
}
