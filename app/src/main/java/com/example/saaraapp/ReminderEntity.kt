package com.example.saaraapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDate

@Entity(tableName = "reminders")
@TypeConverters(TagsConverter::class)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val originalMessage: String,
    val tags: List<String>,
    val category: String,
    val time: Long,
    val reminderDate: Long? = null,     // stored as epoch day (LocalDate.toEpochDay())
    val reminderDateEnd: Long? = null   // end of range, null if single date
)

class TagsConverter {
    @TypeConverter
    fun fromTags(tags: List<String>): String = tags.joinToString(",")

    @TypeConverter
    fun toTags(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(",")
}

fun ReminderEntity.toReminderItem() = ReminderItem(
    id              = id,
    sender          = sender,
    originalMessage = originalMessage,
    tags            = tags,
    category        = ReminderCategory.valueOf(category),
    time            = time,
    reminderDate    = reminderDate?.let { LocalDate.ofEpochDay(it) },
    reminderDateEnd = reminderDateEnd?.let { LocalDate.ofEpochDay(it) }
)

fun ReminderItem.toEntity() = ReminderEntity(
    id              = id,
    sender          = sender,
    originalMessage = originalMessage,
    tags            = tags,
    category        = category.name,
    time            = time,
    reminderDate    = reminderDate?.toEpochDay(),
    reminderDateEnd = reminderDateEnd?.toEpochDay()
)
