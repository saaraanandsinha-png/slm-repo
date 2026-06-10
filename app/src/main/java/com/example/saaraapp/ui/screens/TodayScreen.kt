package com.example.saaraapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saaraapp.NotificationViewModel
import com.example.saaraapp.ReminderItem
import com.example.saaraapp.WhatsAppNotificationScreen
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun TodayScreen(viewModel: NotificationViewModel = viewModel()) {
    val reminders by viewModel.reminders.collectAsState()
    val today = LocalDate.now()

    val todayReminders = remember(reminders) {
        reminders.filter { reminder ->
            val start = reminder.reminderDate
                ?: Date(reminder.time).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val end = reminder.reminderDateEnd
            // Show if today falls anywhere within the reminder's date range
            if (end != null) !today.isBefore(start) && !today.isAfter(end)
            else start == today
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ──────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
            Text(
                text = "Today",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        // ── Content ─────────────────────────────────────────
        if (todayReminders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 52.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Nothing due today!",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "WhatsApp reminders for today\nwill appear here.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "${todayReminders.size} reminder${if (todayReminders.size > 1) "s" else ""} today",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(todayReminders) { reminder ->
                    CalendarReminderCard(reminder, highlight = true)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
