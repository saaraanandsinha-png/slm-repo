package com.example.myapplication

import java.util.UUID

enum class MessageRole {
    USER, ASSISTANT
}

data class ChatMessage(
    val id: UUID = UUID.randomUUID(),
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis()
)
