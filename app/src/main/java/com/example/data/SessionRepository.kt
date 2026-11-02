package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SessionRepository(private val sessionDao: SessionDao) {
    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> =
        sessionDao.getMessagesForSession(sessionId)

    suspend fun getMessagesForSessionSync(sessionId: String): List<ChatMessageEntity> =
        sessionDao.getMessagesForSessionSync(sessionId)

    suspend fun saveMessage(
        sessionId: String,
        speaker: String,
        text: String,
        sessionTitle: String? = null
    ) {
        val message = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            speaker = speaker,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        sessionDao.insertMessage(message)

        val existing = sessionDao.getSessionById(sessionId)
        val newTitle = when {
            existing?.title?.isNotBlank() == true -> existing.title
            sessionTitle?.isNotBlank() == true -> sessionTitle
            speaker == "User" -> text.take(40)
            else -> "Nola Voice Session"
        }
        val updatedSession = SessionEntity(
            sessionId = sessionId,
            title = newTitle,
            startTime = existing?.startTime ?: System.currentTimeMillis(),
            messageCount = (existing?.messageCount ?: 0) + 1,
            lastMessage = text.take(60)
        )
        sessionDao.insertSession(updatedSession)
    }

    suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteSession(sessionId)
        sessionDao.deleteMessagesForSession(sessionId)
    }

    suspend fun clearAll() {
        sessionDao.clearAllSessions()
        sessionDao.clearAllMessages()
    }
}
