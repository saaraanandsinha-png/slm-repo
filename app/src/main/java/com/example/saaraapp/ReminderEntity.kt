package com.example.saaraapp

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "reminders")
@TypeConverters(TagsConverter::class)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val originalMessage: String,
    val tags: List<String>,
    val category: String,
    val time: Long
)

class TagsConverter {
    @TypeConverter
    fun fromTags(tags: List<String>): String = tags.joinToString(",")

    @TypeConverter
    fun toTags(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(",")
}

// Convert between ReminderEntity (database) and ReminderItem (UI)
fun ReminderEntity.toReminderItem() = ReminderItem(
    id       = id,
    sender   = sender,
    originalMessage = originalMessage,
    tags     = tags,
    category = ReminderCategory.valueOf(category),
    time     = time
)

fun ReminderItem.toEntity() = ReminderEntity(
    id       = id,
    sender   = sender,
    originalMessage = originalMessage,
    tags     = tags,
    category = category.name,
    time     = time
)
