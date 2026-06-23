package com.example.saaraapp

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Parses date strings extracted from WhatsApp messages into [LocalDate] values.
 *
 * ## Supported formats
 * - **Relative:** "today", "tomorrow", "tonight", "this week", "next week", "weekend"
 * - **Day of week:** "monday", "mon", "friday", "fri", etc.
 * - **Specific dates:** "12th June", "June 12", "12/6", "12-6", "12 Jun 2025"
 * - **Date ranges:** "June 11-12", "June 11 & 12", "11th to 15th June", "June 11 and June 12"
 *
 * ## Entry points
 * - [extractRangeFrom] — preferred; returns a (start, end) pair for a full message
 * - [extractFrom] — returns the single best date found in a message
 * - [parse] — turns a single tag string (e.g. "tomorrow", "friday") into a date
 *
 * ## Year assumption
 * When only a month+day is given (no year), the current year is assumed.
 * If the resulting date is more than 7 days in the past, the next year is used instead —
 * so "Jan 5" in December will resolve to next January, not last January.
 */
object DateParser {

    private val DAY_MAP = mapOf(
        "monday"    to DayOfWeek.MONDAY,    "mon" to DayOfWeek.MONDAY,
        "tuesday"   to DayOfWeek.TUESDAY,   "tue" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY,
        "thursday"  to DayOfWeek.THURSDAY,  "thu" to DayOfWeek.THURSDAY,
        "friday"    to DayOfWeek.FRIDAY,    "fri" to DayOfWeek.FRIDAY,
        "saturday"  to DayOfWeek.SATURDAY,  "sat" to DayOfWeek.SATURDAY,
        "sunday"    to DayOfWeek.SUNDAY,    "sun" to DayOfWeek.SUNDAY
    )

    // Sub-expressions used to build RANGE_PATTERN below
    private val MONTH_RE = """(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)"""
    private val DAY_RE   = """\d{1,2}(?:st|nd|rd|th)?"""
    private val SEP_RE   = """(?:\s*[-–]\s*|\s*&\s*|\s+(?:and|to|through|till|until)\s+)"""

    /**
     * Matches date ranges within a single message:
     *   Group 1 / 2 / 3  →  pattern A: "june 11-12", "june 11 and 12"
     *   Group 4 / 5 / 6  →  pattern B: "11-12 june", "11 and 12 june"
     */
    private val RANGE_PATTERN = Regex(
        """($MONTH_RE)\s+($DAY_RE)$SEP_RE($DAY_RE)""" +
        """|($DAY_RE)$SEP_RE($DAY_RE)\s+($MONTH_RE)""",
        RegexOption.IGNORE_CASE
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns (startDate, endDate). endDate is null for single-day entries.
     * Handles ranges like "june 11-12", "june 11 & 12", "june 11 and 12",
     * "11-12 june", "june 11 and june 12", "11th to 15th june".
     */
    fun extractRangeFrom(message: String): Pair<LocalDate?, LocalDate?> {

        // 1. Same-month shorthand: "june 11-12", "june 11 & 12", "11-12 june"
        val rangeMatch = RANGE_PATTERN.find(message)
        if (rangeMatch != null) {
            val g = rangeMatch.groupValues
            val (month, day1, day2) = if (g[1].isNotEmpty()) {
                Triple(g[1], g[2], g[3])   // pattern A: month day1 sep day2
            } else {
                Triple(g[6], g[4], g[5])   // pattern B: day1 sep day2 month
            }
            val start = parseSpecificDate("$month $day1")
            val end   = parseSpecificDate("$month $day2")
            if (start != null && end != null && !end.isBefore(start)) {
                return Pair(start, end)
            }
        }

        // 2. Fully-specified range: "june 11 and june 12" (two DATE_PATTERN hits
        //    separated by a range connector)
        val dateMatches = KeywordExtractor.DATE_PATTERN.findAll(message).toList()
        if (dateMatches.size >= 2) {
            val m1 = dateMatches[0]
            val m2 = dateMatches[1]
            val between = message.substring(m1.range.last + 1, m2.range.first).trim()
            val isSep = between.matches(
                Regex("""[-–&,]|and|to|through|till|until""", RegexOption.IGNORE_CASE)
            )
            if (isSep) {
                val start = parseSpecificDate(m1.value)
                val end   = parseSpecificDate(m2.value)
                if (start != null && end != null && !end.isBefore(start)) {
                    return Pair(start, end)
                }
            }
        }

        // 3. Single date (existing logic)
        return Pair(extractFrom(message), null)
    }

    /**
     * Returns the single best date found in the message (used as a convenience
     * and also as the start-date fallback inside extractRangeFrom).
     */
    fun extractFrom(message: String): LocalDate? {
        // Priority 1: specific dates found anywhere in the message
        for (match in KeywordExtractor.DATE_PATTERN.findAll(message)) {
            val date = parseSpecificDate(match.value.trim())
            if (date != null) return date
        }
        // Priority 2: relative / day-of-week words
        for (tag in KeywordExtractor.extractTags(message)) {
            val date = parse(tag)
            if (date != null) return date
        }
        return null
    }

    /** Tries to turn a single tag string into a LocalDate. */
    fun parse(tag: String): LocalDate? {
        val lower = tag.lowercase().trim()
        val today = LocalDate.now()

        return when {
            lower == "today" || lower == "tonight" -> today
            lower == "tomorrow"                    -> today.plusDays(1)
            lower == "this week"                   -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
            lower == "next week"                   -> today.plusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            lower == "weekend"                     -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

            DAY_MAP.containsKey(lower) -> {
                val target    = DAY_MAP[lower]!!
                val daysUntil = (target.value - today.dayOfWeek.value + 7) % 7
                today.plusDays(if (daysUntil == 0) 0L else daysUntil.toLong())
            }

            else -> parseSpecificDate(tag)
        }
    }

    // ── Specific date parser ──────────────────────────────────────────────────

    private fun parseSpecificDate(text: String): LocalDate? {
        val today = LocalDate.now()

        // Strip ordinal suffixes before trying formatters: "12th" → "12", "3rd" → "3"
        val cleaned = text.replace(Regex("""(\d+)(st|nd|rd|th)""", RegexOption.IGNORE_CASE), "$1").trim()

        val formats = listOf(
            "d MMM", "d MMMM",
            "MMM d", "MMMM d",
            "d/M", "d/M/yyyy",
            "d-M", "d-M-yyyy"
        )

        for (format in formats) {
            try {
                val formatter = DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(format)
                    .toFormatter(Locale.ENGLISH)
                val monthDay = MonthDay.parse(cleaned, formatter)
                val date     = monthDay.atYear(today.year)
                return if (date.isBefore(today.minusDays(7))) date.plusYears(1) else date
            } catch (_: Exception) { }
        }
        return null
    }
}
