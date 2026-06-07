package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.ui.components.PreferenceItem
import com.example.myapplication.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var darkMode by remember { mutableStateOf(true) }
    var gpuEnabled by remember { mutableStateOf(true) }
    var markdownEnabled by remember { mutableStateOf(true) }
    
    // SLM Params
    var temperature by remember { mutableFloatStateOf(0.7f) }
    var topP by remember { mutableFloatStateOf(0.9f) }
    var topK by remember { mutableIntStateOf(40) }

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
            SectionHeader("Appearance")
            PreferenceItem(
                title = "Dark Theme",
                subtitle = "Enable dark mode for the interface",
                trailing = { Switch(checked = darkMode, onCheckedChange = { darkMode = it }) }
            )
            PreferenceItem(
                title = "AMOLED Mode",
                subtitle = "Pure black background for OLED screens",
                trailing = { Switch(checked = false, onCheckedChange = { }, enabled = darkMode) }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            SectionHeader("Chat & UI")
            PreferenceItem(
                title = "Markdown Rendering",
                subtitle = "Format bold, italic, and lists",
                trailing = { Switch(checked = markdownEnabled, onCheckedChange = { markdownEnabled = it }) }
            )
            PreferenceItem(
                title = "Font Size",
                subtitle = "Medium (14sp)",
                onClick = { /* Open picker */ }
            )
            PreferenceItem(
                title = "Message Style",
                subtitle = "Modern Bubbles",
                onClick = { /* Open style selector */ }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            SectionHeader("Model Parameters")
            ParameterSlider("Temperature", temperature, 0f..1.5f, "%.1f".format(temperature)) { temperature = it }
            ParameterSlider("Top-P", topP, 0f..1f, "%.2f".format(topP)) { topP = it }
            
            PreferenceItem(
                title = "Top-K",
                subtitle = topK.toString(),
                onClick = { /* Open numeric picker */ }
            )
            
            PreferenceItem(
                title = "Context Length",
                subtitle = "2048 tokens",
                onClick = { /* Open tokens picker */ }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            SectionHeader("Performance")
            PreferenceItem(
                title = "Use GPU (Vulkan/NNAPI)",
                subtitle = "Accelerate model inference",
                trailing = { Switch(checked = gpuEnabled, onCheckedChange = { gpuEnabled = it }) }
            )
            PreferenceItem(
                title = "CPU Threads",
                subtitle = "4 threads",
                onClick = { /* Thread count picker */ }
            )
            PreferenceItem(
                title = "Memory Limit",
                subtitle = "2.5 GB",
                onClick = { /* Set memory cap */ }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ParameterSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(valueLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
