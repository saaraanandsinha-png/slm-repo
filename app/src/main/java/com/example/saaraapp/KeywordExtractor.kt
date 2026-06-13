package com.example.saaraapp

@Suppress("SpellCheckingInspection")
object KeywordExtractor {

    // --- Keyword groups ---
    private val DEADLINE_KEYWORDS = listOf(
        "deadline", "deadlines", "due", "submit", "submission", "last date", "by tonight",
        "by tomorrow", "by friday", "by monday", "by end of day", "before"
    )

    private val ASSIGNMENT_KEYWORDS = listOf(
        "assignment", "assignments", "homework", "task", "project", "report",
        "write up", "writeup", "complete", "finish"
    )

    private val EXAM_KEYWORDS = listOf(
        "exam", "exams", "test", "quiz", "viva", "paper", "mcq", "practical",
        "internal", "mid term", "midterm", "final"
    )

    private val MEETING_KEYWORDS = listOf(
        "meeting", "class", "lecture", "session", "seminar", "workshop",
        "call", "zoom", "online class", "offline class", "attend",
        "planned", "program", "scheduled", "event", "consultation"
    )

    private val REMINDER_KEYWORDS = listOf(
        "don't forget", "dont forget", "remember", "reminder", "bring",
        "carry", "tomorrow", "tonight", "important", "urgent", "must",
        "please note", "note that", "heads up", "fyi", "asap"
    )

    private val SCHEDULE_CHANGE_KEYWORDS = listOf(
        "shifted", "postponed", "rescheduled", "cancelled", "canceled",
        "moved to", "changed to", "new time", "new date", "instead of",
        "not tomorrow", "not today", "delayed", "preponed"
    )

    private val HOLIDAY_KEYWORDS = listOf(
        // General
        "holiday", "holidays", "vacation", "break", "long weekend", "bank holiday",
        "no class", "no school", "no college", "day off",
        // Indian holidays
        "diwali", "holi", "eid", "christmas", "new year", "independence day",
        "republic day", "ganesh chaturthi", "navratri", "durga puja", "onam",
        "pongal", "lohri", "baisakhi", "raksha bandhan", "janmashtami",
        "good friday", "easter", "guru nanak", "ambedkar jayanti", "dussehra"
    )

    private val DAYS_OF_WEEK = listOf(
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
        "mon", "tue", "wed", "thu", "fri", "sat", "sun",
        "today", "tomorrow", "tonight", "this week", "next week",
        "weekend", "weekday"
    )

    // Matches: 12th March, March 12, 5 Jan, 12/03, 03-12-2024
    val DATE_PATTERN = Regex(
        """(\d{1,2}(st|nd|rd|th)?\s*(jan(uary)?|feb(ruary)?|mar(ch)?|apr(il)?|may|jun(e)?|jul(y)?|aug(ust)?|sep(t(ember)?)?|oct(ober)?|nov(ember)?|dec(ember)?)""" +
        """|(jan(uary)?|feb(ruary)?|mar(ch)?|apr(il)?|may|jun(e)?|jul(y)?|aug(ust)?|sep(t(ember)?)?|oct(ober)?|nov(ember)?|dec(ember)?)\s*\d{1,2}(st|nd|rd|th)?""" +
        """|\d{1,2}[/\-]\d{1,2}([/\-]\d{2,4})?)""",
        RegexOption.IGNORE_CASE
    )

    // Matches: 3pm, 3:00, 3:00pm, at 5, at 5:30pm
    private val TIME_PATTERN = Regex(
        """(?:\d{1,2}:\d{2}\s*(?:am|pm)|\d{1,2}\s*(?:am|pm)|at\s+\d{1,2}(?::\d{2})?)""",
        RegexOption.IGNORE_CASE
    )

    /** Returns true if the message contains any relevant keyword, date, or time */
    fun isRelevant(text: String): Boolean {
        val lower = text.lowercase()
        val allKeywords = DEADLINE_KEYWORDS + ASSIGNMENT_KEYWORDS + EXAM_KEYWORDS +
                MEETING_KEYWORDS + REMINDER_KEYWORDS + SCHEDULE_CHANGE_KEYWORDS +
                HOLIDAY_KEYWORDS + DAYS_OF_WEEK
        return allKeywords.any { lower.contains(it) } ||
                DATE_PATTERN.containsMatchIn(text) ||
                TIME_PATTERN.containsMatchIn(text)
    }

    /** Returns a list of matched tags (keywords, days, dates, times) found in the text */
    fun extractTags(text: String): List<String> {
        val lower = text.lowercase()
        val tags = mutableListOf<String>()

        val allKeywords = DEADLINE_KEYWORDS + ASSIGNMENT_KEYWORDS + EXAM_KEYWORDS +
                MEETING_KEYWORDS + REMINDER_KEYWORDS + SCHEDULE_CHANGE_KEYWORDS +
                HOLIDAY_KEYWORDS + DAYS_OF_WEEK

        allKeywords.forEach { keyword ->
            if (lower.contains(keyword)) {
                tags.add(keyword.replaceFirstChar { it.uppercase() })
            }
        }

        DATE_PATTERN.findAll(text).forEach { tags.add(it.value.trim()) }
        TIME_PATTERN.findAll(text).forEach { tags.add(it.value.trim()) }

        return tags.distinct()
    }

    /** Figures out which category best fits the message */
    fun categorize(text: String): ReminderCategory {
        val lower = text.lowercase()
        return when {
            SCHEDULE_CHANGE_KEYWORDS.any { lower.contains(it) } -> ReminderCategory.SCHEDULE_CHANGE
            HOLIDAY_KEYWORDS.any { lower.contains(it) }         -> ReminderCategory.EVENT
            DEADLINE_KEYWORDS.any { lower.contains(it) }        -> ReminderCategory.ACADEMIC
            ASSIGNMENT_KEYWORDS.any { lower.contains(it) }      -> ReminderCategory.ACADEMIC
            EXAM_KEYWORDS.any { lower.contains(it) }            -> ReminderCategory.ACADEMIC
            MEETING_KEYWORDS.any { lower.contains(it) }         -> ReminderCategory.ACADEMIC
            REMINDER_KEYWORDS.any { lower.contains(it) }        -> ReminderCategory.PERSONAL
            else                                                 -> ReminderCategory.OTHER
        }
    }
}
