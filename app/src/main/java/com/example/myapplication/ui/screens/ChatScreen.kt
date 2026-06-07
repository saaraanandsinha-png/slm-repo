package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.ChatMessage
import com.example.myapplication.MessageRole
import com.example.myapplication.ui.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    "SLM Assistant",
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                NavigationDrawerItem(
                    label = { Text("New Chat") },
                    selected = false,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = { 
                        messages.clear()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp))
                
                NavigationDrawerItem(
                    label = { Text("Model Management") },
                    selected = false,
                    icon = { Icon(Icons.Default.Download, contentDescription = null) },
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.ModelManagement) 
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Benchmark Dashboard") },
                    selected = false,
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Benchmark) 
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Chat History") },
                    selected = false,
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.ChatHistory) 
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Settings) 
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text("Qwen 2.5 1.5B", style = MaterialTheme.typography.titleMedium)
                                Text("Local inference active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = { navController.navigate(Screen.ChatHistory) }) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "History")
                            }
                            IconButton(onClick = { /* More options */ }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More")
                            }
                        }
                    )
                    // Model Selector Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Model:", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.width(8.dp))
                            AssistChip(
                                onClick = { /* Select model */ },
                                label = { Text("Qwen 2.5 1.5B (Q4_K_M)") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            },
            bottomBar = {
                ChatInput(
                    value = inputText,
                    onValueChange = { inputText = it },
                    isGenerating = isGenerating,
                    onSend = {
                        if (inputText.isNotBlank()) {
                            val userText = inputText
                            messages.add(ChatMessage(content = userText, role = MessageRole.USER))
                            inputText = ""
                            isGenerating = true
                            
                            // Simulate streaming response
                            scope.launch {
                                val assistantMessageId = java.util.UUID.randomUUID()
                                var assistantContent = ""
                                messages.add(ChatMessage(id = assistantMessageId, content = "", role = MessageRole.ASSISTANT))
                                
                                val responseText = "As a local Small Language Model (SLM), I'm running directly on your device. I can assist with code, creative writing, and data analysis without sending your data to any servers."
                                val tokens = responseText.split(" ")
                                
                                for (token in tokens) {
                                    delay(80.milliseconds) // Simulate token latency
                                    assistantContent += "$token "
                                    val index = messages.indexOfFirst { it.id == assistantMessageId }
                                    if (index != -1) {
                                        messages[index] = messages[index].copy(content = assistantContent.trim())
                                    }
                                }
                                isGenerating = false
                            }
                        }
                    },
                    onStop = { isGenerating = false }
                )
            }
        ) { innerPadding ->
            ChatList(
                messages = messages,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun ChatList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when messages change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            ChatBubble(message)
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ))
                .background(containerColor)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isUser) "You" else "Assistant",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.6f)
                )
                if (!isUser) {
                    IconButton(onClick = { /* Copy */ }, modifier = Modifier.size(16.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(12.dp))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            
            // Check for code-like blocks (very basic simulation)
            if (message.content.contains("```") || message.content.length > 100 && message.role == MessageRole.ASSISTANT && message.content.contains("code")) {
                CodeBlock(message.content)
            } else {
                Text(
                    text = message.content,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun CodeBlock(content: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("kotlin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text("Copy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = content.replace("```", ""),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {

            IconButton(
                onClick = { /* Attach */ }
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Attach"
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 5,
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            "Ask anything...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )

            if (isGenerating) {

                FilledIconButton(
                    onClick = onStop,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop"
                    )
                }

            } else {

                IconButton(
                    onClick = { /* Voice */ }
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Voice"
                    )
                }

                FilledIconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}