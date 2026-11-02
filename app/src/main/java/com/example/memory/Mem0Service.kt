package com.example.memory

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "Mem0Service"

data class MemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val source: String = "mem0",
    val timestamp: Long = System.currentTimeMillis()
)

class Mem0Service(
    private var apiKey: String = "m0-8DbLHr53LfqrdtcIgTFq8VTOSFZh4HIzSMYpLN1y",
    private val userId: String = "nola_user"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Local cache / graceful fallback store when offline or if API key expires/depletes
    private val localFallbackMemories = mutableListOf<MemoryItem>()

    fun setApiKey(newKey: String) {
        if (newKey.isNotBlank()) {
            this.apiKey = newKey.trim()
        }
    }

    /**
     * Search memories relevant to the user query to augment Gemini context.
     */
    suspend fun searchMemories(query: String): List<MemoryItem> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext localFallbackMemories.take(5)

        try {
            val payload = JSONObject().apply {
                put("query", query)
                put("user_id", userId)
            }
            val request = Request.Builder()
                .url("https://api.mem0.ai/v1/memories/search/")
                .header("Authorization", "Token $apiKey")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotBlank()) {
                    val resultList = mutableListOf<MemoryItem>()
                    // Mem0 search returns a JSON array or object with "results"
                    if (body.trim().startsWith("[")) {
                        val array = JSONArray(body)
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val memoryText = obj.optString("memory", obj.optString("text", ""))
                            if (memoryText.isNotBlank()) {
                                resultList.add(MemoryItem(id = obj.optString("id", UUID.randomUUID().toString()), text = memoryText))
                            }
                        }
                    } else {
                        val root = JSONObject(body)
                        val array = root.optJSONArray("results") ?: JSONArray()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val memoryText = obj.optString("memory", obj.optString("text", ""))
                            if (memoryText.isNotBlank()) {
                                resultList.add(MemoryItem(id = obj.optString("id", UUID.randomUUID().toString()), text = memoryText))
                            }
                        }
                    }
                    if (resultList.isNotEmpty()) return@withContext resultList
                } else {
                    Log.w(TAG, "Mem0 search returned code ${response.code}: $body. Using local memory.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Mem0 search error, falling back to local memory: ${e.message}")
        }

        // Graceful fallback to local memories
        return@withContext localFallbackMemories.filter { it.text.contains(query, ignoreCase = true) }
            .ifEmpty { localFallbackMemories.take(5) }
    }

    /**
     * Persist a conversation turn into Mem0.
     */
    suspend fun addMemory(userMessage: String, agentResponse: String): Boolean = withContext(Dispatchers.IO) {
        val memoryNote = "User said: '$userMessage'. Nola answered: '$agentResponse'"
        localFallbackMemories.add(0, MemoryItem(text = memoryNote, source = "local"))

        if (apiKey.isBlank()) return@withContext true

        try {
            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
                put(JSONObject().apply {
                    put("role", "assistant")
                    put("content", agentResponse)
                })
            }

            val payload = JSONObject().apply {
                put("messages", messagesArray)
                put("user_id", userId)
            }

            val request = Request.Builder()
                .url("https://api.mem0.ai/v1/memories/")
                .header("Authorization", "Token $apiKey")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Log.d(TAG, "Saved memory to Mem0 successfully.")
                    return@withContext true
                } else {
                    Log.w(TAG, "Failed to save to Mem0 (code ${response.code}): $body")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception saving to Mem0: ${e.message}")
        }
        return@withContext false
    }

    /**
     * Fetch all active memories for display in UI.
     */
    suspend fun getAllMemories(): List<MemoryItem> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext localFallbackMemories

        try {
            val request = Request.Builder()
                .url("https://api.mem0.ai/v1/memories/?user_id=$userId")
                .header("Authorization", "Token $apiKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotBlank()) {
                    val list = mutableListOf<MemoryItem>()
                    if (body.trim().startsWith("[")) {
                        val array = JSONArray(body)
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val text = obj.optString("memory", obj.optString("text", ""))
                            if (text.isNotBlank()) {
                                list.add(MemoryItem(id = obj.optString("id", UUID.randomUUID().toString()), text = text, source = "mem0"))
                            }
                        }
                    }
                    if (list.isNotEmpty()) {
                        return@withContext list
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Mem0 getAllMemories failed: ${e.message}")
        }
        return@withContext localFallbackMemories
    }
}
