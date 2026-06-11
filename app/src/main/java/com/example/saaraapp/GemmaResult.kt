package com.example.saaraapp

/**
 * Structured result returned by FunctionGemmaHelper after analyzing a WhatsApp message.
 *
 * @param isReminder  Whether the model thinks this message is a reminder worth saving.
 * @param category    The best-fit category (EXAM, DEADLINE, MEETING, etc.).
 * @param dateText    Raw date string extracted by the model (e.g. "Friday", "12th June").
 * @param timeText    Raw time string extracted by the model (e.g. "5:00 PM").
 * @param tags        List of relevant keywords/tags found in the message.
 * @param fromFallback  True if the result came from KeywordExtractor (model not ready yet).
 */
data class GemmaResult(
    val isReminder: Boolean,
    val category: ReminderCategory,
    val dateText: String?,
    val timeText: String?,
    val tags: List<String>,
    val fromFallback: Boolean = false
)
