package com.example.saaraapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.example.saaraapp.ReminderCategory
import com.example.saaraapp.ReminderItem
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

// Colour for each category (same as reminder screen)
private fun categoryColor(category: ReminderCategory): Color = when (category) {
    ReminderCategory.DEADLINE        -> Color(0xFFE53935)
    ReminderCategory.ASSIGNMENT      -> Color(0xFF1E88E5)
    ReminderCategory.EXAM            -> Color(0xFF43A047)
    ReminderCategory.MEETING         -> Color(0xFF00897B)
    ReminderCategory.REMINDER        -> Color(0xFFFB8C00)
    ReminderCategory.SCHEDULE_CHANGE -> Color(0xFF6D4C41)
    ReminderCategory.HOLIDAY         -> Color(0xFFE91E8C)
    ReminderCategory.OTHER           -> Color(0xFF546E7A)
}

@Composable
fun CalendarScreen(viewModel: NotificationViewModel = viewModel()) {
    val reminders by viewModel.reminders.collectAsState()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Group reminders by date
    val remindersByDate: Map<LocalDate, List<ReminderItem>> = remember(reminders) {
        reminders.groupBy { reminder ->
            reminder.reminderDate ?: reminder.time.toLocalDate()
        }
    }

    // Reminders for the selected day
    val selectedDayReminders = remindersByDate[selectedDate] ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Month header ──────────────────────────────────────
        MonthHeader(
            currentMonth = currentMonth,
            onPrevious   = { currentMonth = currentMonth.minusMonths(1) },
            onNext        = { currentMonth = currentMonth.plusMonths(1) }
        )

        // ── Day of week labels ────────────────────────────────
        DayOfWeekRow()

        // ── Calendar grid ─────────────────────────────────────
        CalendarGrid(
            currentMonth     = currentMonth,
            selectedDate     = selectedDate,
            remindersByDate  = remindersByDate,
            onDateSelected   = { selectedDate = it }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ── Selected day reminders ────────────────────────────
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.width(8.dp))
            if (selectedDayReminders.isNotEmpty()) {
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
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedDayReminders) { reminder ->
                    CalendarReminderCard(reminder)
                }
            }
        }
    }
}

// ── Month header with prev/next arrows ───────────────────────────────────────
@Composable
fun MonthHeader(
    currentMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
        }
        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
        }
    }
}

// ── Mon Tue Wed ... row ───────────────────────────────────────────────────────
@Composable
fun DayOfWeekRow() {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        days.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(Modifier.height(4.dp))
}

// ── Calendar grid ─────────────────────────────────────────────────────────────
@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    remindersByDate: Map<LocalDate, List<ReminderItem>>,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDay     = currentMonth.atDay(1)
    // How many empty cells before day 1 (Mon=0, Tue=1 ... Sun=6)
    val startOffset  = (firstDay.dayOfWeek.value - 1)
    val daysInMonth  = currentMonth.lengthOfMonth()
    val today        = LocalDate.now()

    val cells = startOffset + daysInMonth
    val rows  = (cells + 6) / 7

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val index = row * 7 + col
                    val day   = index - startOffset + 1

                    if (day < 1 || day > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val date      = currentMonth.atDay(day)
                        val isToday   = date == today
                        val isSelected = date == selectedDate
                        val dots      = remindersByDate[date]?.map { categoryColor(it.category) } ?: emptyList()

                        DayCell(
                            day        = day,
                            isToday    = isToday,
                            isSelected = isSelected,
                            dots       = dots,
                            modifier   = Modifier.weight(1f),
                            onClick    = { onDateSelected(date) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ── Single day cell ───────────────────────────────────────────────────────────
@Composable
fun DayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    dots: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday    -> MaterialTheme.colorScheme.primaryContainer
        else       -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday    -> MaterialTheme.colorScheme.onPrimaryContainer
        else       -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            fontSize = 14.sp,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
        // Coloured dots for reminders
        if (dots.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                dots.take(3).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else color)
                    )
                }
            }
        }
    }
}

// ── Reminder card for selected day ────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarReminderCard(reminder: ReminderItem) {
    val color = categoryColor(reminder.category)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Coloured left bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 48.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                // ── Category + sender ────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${reminder.category.emoji} ${reminder.category.label}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "· ${reminder.sender}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                // ── Message body ─────────────────────────────
                Text(
                    text = reminder.originalMessage,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                // ── Tags ─────────────────────────────────────
                if (reminder.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement   = Arrangement.spacedBy(4.dp)
                    ) {
                        reminder.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helper: Long → LocalDate ──────────────────────────────────────────────────
private fun Long.toLocalDate(): LocalDate =
    Date(this).toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
