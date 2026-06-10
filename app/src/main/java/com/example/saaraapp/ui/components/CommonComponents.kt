package com.example.saaraapp.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * Returns an [AnnotatedString] with every tag occurrence in [message] highlighted
 * using [highlightColor] (bold + colored text, no background chip).
 */
fun buildHighlightedMessage(message: String, tags: List<String>, highlightColor: Color): AnnotatedString {
    return buildAnnotatedString {
        append(message)
        val lower = message.lowercase()
        tags.forEach { tag ->
            val tagLower = tag.lowercase()
            var start = lower.indexOf(tagLower)
            while (start >= 0) {
                addStyle(
                    style = SpanStyle(
                        color      = highlightColor,
                        fontWeight = FontWeight.SemiBold
                    ),
                    start = start,
                    end   = start + tag.length
                )
                start = lower.indexOf(tagLower, start + 1)
            }
        }
    }
}

@Composable
fun PreferenceItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {},
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = subtitle?.let { { Text(it) } },
            trailingContent = trailing
        )
    }
}
