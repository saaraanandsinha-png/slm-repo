package com.example.saaraapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saaraapp.NotificationViewModel
import com.example.saaraapp.ReminderItem
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.util.*

@Composable
fun WeekScreen(viewModel: NotificationViewModel = viewModel()) {
    val reminders by viewModel.reminders.collectAsState()

    // Start of current week (Monday)
    var weekStart by remember {
        mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
    val weekEnd  = weekStart.plusDays(6)

    // Group reminders by date — ranged reminders appear on every day in their range
    val remindersByDate: Map<LocalDate, List<ReminderItem>> = remember(reminders) {
        val map = mutableMapOf<LocalDate, MutableList<ReminderItem>>()
        reminders.forEach { reminder ->
            val start = reminder.reminderDate
                ?: Date(reminder.time).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val end = reminder.reminderDateEnd
            if (end != null && !end.isBefore(start)) {
                var d = start
                while (!d.isAfter(end)) {
                    map.getOrPut(d) { mutableListOf() }.add(reminder)
                    d = d.plusDays(1)
                }
            } else {
                map.getOrPut(start) { mutableListOf() }.add(reminder)
            }
        }
        map
    }

    val selectedDayReminders = remindersByDate[selectedDate] ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Week range header with arrows ────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                weekStart    = weekStart.minusWeeks(1)
                selectedDate = selectedDate.minusWeeks(1)
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous week")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Week of ${weekStart.format(DateTimeFormatter.ofPattern("d MMM"))}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "${weekStart.format(DateTimeFormatter.ofPattern("d MMM"))} – ${weekEnd.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = {
                weekStart    = weekStart.plusWeeks(1)
                selectedDate = selectedDate.plusWeeks(1)
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next week")
            }
        }

        // ── Day spread: Mon Tue Wed Thu Fri Sat Sun ──────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekDays.forEach { date ->
                val isSelected = date == selectedDate
                val isToday    = date == LocalDate.now()
                val hasDots    = remindersByDate[date]?.isNotEmpty() == true

                val bgColor = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday    -> MaterialTheme.colorScheme.primaryContainer
                    else       -> MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday    -> MaterialTheme.colorScheme.onPrimaryContainer
                    else       -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { selectedDate = date }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EEE")),
                        fontSize = 11.sp,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(Modifier.height(4.dp))
                    // Dot if this day has reminders
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasDots) {
                                    if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.primary
                                } else Color.Transparent
                            )
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()

        // ── Selected day reminders ───────────────────────────
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            if (selectedDayReminders.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Badge { Text("${selectedDayReminders.size}") }
            }
        }

        if (selectedDayReminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No reminders on this day",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(selectedDayReminders) { reminder ->
                    CalendarReminderCard(reminder)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
