package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val speaker: String, // "User", "Agent", "System"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
