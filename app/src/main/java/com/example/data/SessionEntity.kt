package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val title: String,
    val startTime: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val lastMessage: String = ""
)
