package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class HistoryItem(
    val title: String,
    val lastMessage: String,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(navController: NavController) {
    val historyItems = remember {
        listOf(
            HistoryItem("Android Project", "How do I implement Hilt?", "Today"),
            HistoryItem("Research Notes", "Summary of SLM papers", "Today"),
            HistoryItem("AI Benchmark", "Comparison of Llama and Qwen", "Yesterday"),
            HistoryItem("Travel Plans", "Best places in Tokyo", "Oct 20")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            items(historyItems) { item ->
                ListItem(
                    headlineContent = { Text(item.title) },
                    supportingContent = { Text(item.lastMessage, maxLines = 1) },
                    trailingContent = {
                        Column {
                            Text(item.date, style = MaterialTheme.typography.labelSmall)
                            IconButton(onClick = { /* Menu */ }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }
                        }
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                HorizontalDivider()
            }
        }
    }
}
