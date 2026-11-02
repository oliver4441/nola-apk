package com.example.ai

import android.util.Log
import com.example.memory.MemoryItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiVoiceService"

class GeminiVoiceService {

    // Lazy initialization of Firebase AI Logic Gemini 3.8 Flash model
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are Nola, a highly capable, articulate, and empathetic real-time AI voice companion running on LiveKit. " +
                    "Your responses will be spoken aloud to the user on their phone. Keep answers concise, natural, conversational, and " +
                    "directly relevant (under 3 sentences when possible). Do not use emojis, markdown tables, or bullet lists."
                )
            }
        )
    }

    /**
     * Generates an intelligent voice response using Gemini 3.8 Flash with Mem0 contextual memory.
     */
    suspend fun generateVoiceResponse(
        userPrompt: String,
        relevantMemories: List<MemoryItem> = emptyList(),
        recentTranscripts: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val memoryContext = if (relevantMemories.isNotEmpty()) {
            "Relevant memories from Mem0:\n" + relevantMemories.joinToString("\n") { "- ${it.text}" } + "\n\n"
        } else ""

        val historyContext = if (recentTranscripts.isNotEmpty()) {
            "Recent dialogue:\n" + recentTranscripts.takeLast(4).joinToString("\n") { (speaker, text) -> "$speaker: $text" } + "\n\n"
        } else ""

        val fullPrompt = "${memoryContext}${historyContext}User said: $userPrompt\nNola:"

        try {
            withTimeout(60_000L) {
                val response = model.generateContent(fullPrompt)
                val text = response.text
                if (!text.isNullOrBlank()) {
                    text.trim()
                } else {
                    "I heard you clearly, and I am ready with the next step."
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation error: ${e.message}", e)
            "Understood. I received your request: $userPrompt."
        }
    }

    /**
     * Streams voice response chunks from Gemini for low Time-to-First-Token (TTFT).
     */
    fun streamVoiceResponse(
        userPrompt: String,
        relevantMemories: List<MemoryItem> = emptyList()
    ): Flow<String> = flow {
        val memoryContext = if (relevantMemories.isNotEmpty()) {
            "Mem0 Memories:\n" + relevantMemories.joinToString("\n") { "- ${it.text}" } + "\n\n"
        } else ""

        val fullPrompt = "${memoryContext}User said: $userPrompt\nNola:"
        try {
            withTimeout(60_000L) {
                model.generateContentStream(fullPrompt).collect { chunk ->
                    val chunkText = chunk.text
                    if (!chunkText.isNullOrEmpty()) {
                        emit(chunkText)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini stream error: ${e.message}", e)
            emit("I am processing your command: $userPrompt")
        }
    }.flowOn(Dispatchers.IO)
}
