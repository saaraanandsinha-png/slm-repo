package com.example.saaraapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saaraapp.FunctionGemmaHelper
import com.example.saaraapp.NotificationViewModel
import com.example.saaraapp.ReminderItem
import com.example.saaraapp.toLocalDate
import java.time.LocalDate
import java.util.Calendar

@Composable
fun TodayScreen(viewModel: NotificationViewModel = viewModel()) {
    val reminders by viewModel.reminders.collectAsState()
    val isModelReady by FunctionGemmaHelper.isReady.collectAsState()
    val today = LocalDate.now()
    val context = LocalContext.current

    // Reminder selected for time picking
    var reminderForTimePicker by remember { mutableStateOf<ReminderItem?>(null) }
    val timePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0)

    // Time picker dialog
    reminderForTimePicker?.let { reminder ->
        AlertDialog(
            onDismissRequest = { reminderForTimePicker = null },
            title = { Text("Set reminder time") },
            text  = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        val date = reminder.reminderDate ?: today
                        set(date.year, date.monthValue - 1, date.dayOfMonth,
                            timePickerState.hour, timePickerState.minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    viewModel.scheduleAlarm(context, reminder, cal.timeInMillis)
                    reminderForTimePicker = null
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { reminderForTimePicker = null }) { Text("Cancel") }
            }
        )
    }
    val todayReminders = remember(reminders) {
        reminders.filter { reminder ->
            val start = reminder.reminderDate ?: reminder.time.toLocalDate()
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
                text = "Inbox",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "All captured WhatsApp messages",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        // ── Model loading banner ─────────────────────────────
        AnimatedVisibility(
            visible = !isModelReady,
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            Surface(
                color    = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text  = "Model loading — reminders will be AI-classified once ready",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // ── Content ─────────────────────────────────────────
        if (todayReminders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📬", fontSize = 52.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No messages captured yet",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = "${todayReminders.size} message${if (todayReminders.size > 1) "s" else ""} captured",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(todayReminders) { reminder ->
                    CalendarReminderCard(
                        reminder          = reminder,
                        highlight         = true,
                        onSetReminder     = { reminderForTimePicker = reminder },
                        onDismissReminder = { viewModel.dismissReminderPrompt(reminder.id) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
