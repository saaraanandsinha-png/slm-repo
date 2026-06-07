package com.example.myapplication

import java.util.UUID

data class TodoItem(
    val id: UUID = UUID.randomUUID(),
    val task: String,
    val isDone: Boolean = false
)
