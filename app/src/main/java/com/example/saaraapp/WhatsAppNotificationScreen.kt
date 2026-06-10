package com.example.saaraapp

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saaraapp.ui.components.buildHighlightedMessage
import java.text.SimpleDateFormat
import java.util.*

// Colour for each category badge
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
fun WhatsAppNotificationScreen(modifier: Modifier = Modifier, viewModel: NotificationViewModel = viewModel()) {
    val context   = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    var isGranted by remember { mutableStateOf(false) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val listeners = Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            )
            isGranted = listeners?.contains(context.packageName) == true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "📬 WhatsApp Reminders",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Important messages, automatically extracted",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        when {
            !isGranted -> PermissionCard(onGrant = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            })

            reminders.isEmpty() -> EmptyState()

            else -> {
                Text(
                    text = "${reminders.size} reminder${if (reminders.size > 1) "s" else ""} found",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(reminders) { reminder ->
                        ReminderCard(reminder)
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔒", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Notification Access Required",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "This app needs permission to read your WhatsApp notifications and extract reminders.",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onGrant) { Text("Grant Permission") }
        }
    }
}

@Composable
fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📭", fontSize = 52.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No reminders yet",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "When someone sends you a WhatsApp\nmessage with a deadline, assignment,\ndate, or reminder — it'll appear here.",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ReminderCard(reminder: ReminderItem) {
    val time = remember(reminder.time) {
        SimpleDateFormat("EEE, d MMM · hh:mm a", Locale.ENGLISH).format(Date(reminder.time))
    }
    val badgeColor = categoryColor(reminder.category)
    val highlightedMessage = buildHighlightedMessage(
        message        = reminder.originalMessage,
        tags           = reminder.tags,
        highlightColor = badgeColor
    )

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Category badge + time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = "${reminder.category.emoji}  ${reminder.category.label}",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = badgeColor
                    )
                }
                Text(
                    text  = time,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            // Sender
            Text(
                text       = "From: ${reminder.sender}",
                fontWeight = FontWeight.Bold,
                fontSize   = 14.sp,
                color      = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(6.dp))

            // Full message with keywords highlighted inline
            Text(
                text       = highlightedMessage,
                fontSize   = 14.sp,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}
