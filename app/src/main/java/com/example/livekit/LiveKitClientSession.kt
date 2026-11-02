package com.example.livekit

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "LiveKitSession"

enum class LiveKitConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting
}

data class LiveKitRoomInfo(
    val serverUrl: String,
    val roomName: String,
    val participantIdentity: String,
    val isCloud: Boolean
)

class LiveKitClientSession(
    private val scope: CoroutineScope,
    private val onMessageReceived: (String) -> Unit,
    private val onAgentSpeaking: (Boolean) -> Unit,
    private val onStateChanged: (LiveKitConnectionState) -> Unit
) {
    private val _connectionState = MutableStateFlow(LiveKitConnectionState.Disconnected)
    val connectionState: StateFlow<LiveKitConnectionState> = _connectionState.asStateFlow()

    private val _roomInfo = MutableStateFlow<LiveKitRoomInfo?>(null)
    val roomInfo: StateFlow<LiveKitRoomInfo?> = _roomInfo.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive WebSocket stream
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null

    fun connect(
        url: String,
        token: String,
        roomName: String = "nola-agent-room",
        identity: String = "user-mobile"
    ) {
        disconnect()

        _connectionState.value = LiveKitConnectionState.Connecting
        onStateChanged(LiveKitConnectionState.Connecting)

        val cleanUrl = url.trim().trimEnd('/')
        // Standard LiveKit WebRTC signaling endpoint: /rtc?access_token=...
        val wsUrl = if (cleanUrl.contains("/rtc")) {
            if (cleanUrl.contains("access_token=")) cleanUrl else "$cleanUrl?access_token=$token&auto_subscribe=1"
        } else {
            "$cleanUrl/rtc?access_token=$token&auto_subscribe=1"
        }

        val isCloud = cleanUrl.contains("livekit.cloud")
        _roomInfo.value = LiveKitRoomInfo(
            serverUrl = cleanUrl,
            roomName = roomName,
            participantIdentity = identity,
            isCloud = isCloud
        )

        Log.d(TAG, "Connecting to LiveKit Cloud: $cleanUrl (Room: $roomName)")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to LiveKit signaling channel successfully.")
                scope.launch(Dispatchers.Main) {
                    _connectionState.value = LiveKitConnectionState.Connected
                    onStateChanged(LiveKitConnectionState.Connected)
                    startHeartbeat()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch(Dispatchers.Main) {
                    parseSignalMessage(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "LiveKit WebSocket closing: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "LiveKit WebSocket closed: $code / $reason")
                scope.launch(Dispatchers.Main) {
                    _connectionState.value = LiveKitConnectionState.Disconnected
                    onStateChanged(LiveKitConnectionState.Disconnected)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "LiveKit WebSocket failure: ${t.message}")
                scope.launch(Dispatchers.Main) {
                    _connectionState.value = LiveKitConnectionState.Disconnected
                    onStateChanged(LiveKitConnectionState.Disconnected)
                }
            }
        })
    }

    private fun parseSignalMessage(text: String) {
        try {
            if (text.startsWith("{")) {
                val json = JSONObject(text)
                // Handle LiveKit Signal Responses: join, active speakers, data packets
                if (json.has("speakers")) {
                    val speakers = json.getJSONArray("speakers")
                    val isAnySpeaking = speakers.length() > 0
                    onAgentSpeaking(isAnySpeaking)
                }
                if (json.has("data")) {
                    val data = json.getString("data")
                    onMessageReceived(data)
                }
            } else {
                onMessageReceived(text)
            }
        } catch (_: Exception) {
            onMessageReceived(text)
        }
    }

    private fun startHeartbeat() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive && _connectionState.value == LiveKitConnectionState.Connected) {
                delay(20000)
                try {
                    val pingJson = JSONObject().apply {
                        put("type", "ping")
                        put("timestamp", System.currentTimeMillis())
                    }
                    webSocket?.send(pingJson.toString())
                } catch (_: Exception) {}
            }
        }
    }

    fun sendData(message: String) {
        scope.launch(Dispatchers.IO) {
            try {
                webSocket?.send(message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send data: ${e.message}")
            }
        }
    }

    fun disconnect() {
        pingJob?.cancel()
        pingJob = null
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {}
        webSocket = null
        _connectionState.value = LiveKitConnectionState.Disconnected
        onStateChanged(LiveKitConnectionState.Disconnected)
    }
}
