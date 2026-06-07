package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(navController: NavController) {
    val tpsProducer = remember { CartesianChartModelProducer() }
    val tempProducer = remember { CartesianChartModelProducer() }
    val ramProducer = remember { CartesianChartModelProducer() }
    
    LaunchedEffect(Unit) {
        tpsProducer.runTransaction {
            lineSeries { series(20, 25, 22, 28, 26, 30, 27, 29, 31, 28) }
        }
        tempProducer.runTransaction {
            lineSeries { series(38, 39, 41, 42, 42, 43, 44, 44, 45, 43) }
        }
        ramProducer.runTransaction {
            lineSeries { series(1.2, 1.4, 1.7, 1.8, 1.8, 1.9, 1.8, 1.8, 1.8, 1.8) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Benchmark") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Start */ }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run Benchmark")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Model: Qwen 2.5 1.5B (Active)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Metrics Summary
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCard("FTL (First Token)", "312 ms", Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                MetricCard("TPS (Avg)", "26.3", Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCard("Peak RAM", "1.9 GB", Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                MetricCard("Max Temp", "45°C", Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            BenchmarkChartSection("Tokens Per Second (TPS)", tpsProducer)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            BenchmarkChartSection("Temperature (°C)", tempProducer)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            BenchmarkChartSection("Memory Usage (GB)", ramProducer)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Device: Android Phone (Snapdragon 8 Gen 2)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BenchmarkChartSection(title: String, producer: CartesianChartModelProducer) {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Spacer(modifier = Modifier.height(8.dp))
    Box(modifier = Modifier.height(180.dp).fillMaxWidth()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
            ),
            modelProducer = producer,
        )
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
