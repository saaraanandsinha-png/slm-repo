package com.example.saaraapp

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DateParser {

    private val DAY_MAP = mapOf(
        "monday" to DayOfWeek.MONDAY,   "mon" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY,   "thu" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY,   "fri" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY,   "sat" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY,   "sun" to DayOfWeek.SUNDAY
    )

    /**
     * Tries to turn a tag string into a LocalDate.
     * Returns null if the tag is a time (e.g. "5pm") or unrecognised.
     */
    fun parse(tag: String): LocalDate? {
        val lower = tag.lowercase().trim()
        val today = LocalDate.now()

        return when {
            lower == "today" || lower == "tonight"  -> today
            lower == "tomorrow"                     -> today.plusDays(1)
            lower == "this week"                    -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
            lower == "next week"                    -> today.plusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            lower == "weekend"                      -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

            // Day of week → next occurrence (or today if it is that day)
            DAY_MAP.containsKey(lower) -> {
                val target     = DAY_MAP[lower]!!
                val daysUntil  = (target.value - today.dayOfWeek.value + 7) % 7
                today.plusDays(if (daysUntil == 0) 0L else daysUntil.toLong())
            }

            // Specific date like "12th March", "March 12", "12/03"
            else -> parseSpecificDate(tag)
        }
    }

    /**
     * Given a full message, returns the best LocalDate found in it.
     * Checks the extracted tags in priority order.
     */
    fun extractFrom(message: String): LocalDate? {
        val tags = KeywordExtractor.extractTags(message)
        for (tag in tags) {
            val date = parse(tag)
            if (date != null) return date
        }
        return null
    }

    // ── Specific date parser ──────────────────────────────────────────────────
    private fun parseSpecificDate(text: String): LocalDate? {
        val today = LocalDate.now()

        // Strip ordinal suffixes: "12th" → "12", "3rd" → "3"
        val cleaned = text.replace(Regex("(\\d+)(st|nd|rd|th)", RegexOption.IGNORE_CASE), "$1").trim()

        val formats = listOf(
            "d MMM", "d MMMM",
            "MMM d", "MMMM d",
            "d/M", "d/M/yyyy",
            "d-M", "d-M-yyyy"
        )

        for (format in formats) {
            try {
                val formatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH)
                // MonthDay handles dates without year
                val monthDay  = MonthDay.parse(cleaned, formatter)
                val date      = monthDay.atYear(today.year)
                // If date already passed by more than 7 days → next year
                return if (date.isBefore(today.minusDays(7))) date.plusYears(1) else date
            } catch (_: Exception) { }
        }
        return null
    }
}
