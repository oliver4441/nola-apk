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

    // Configured with pingInterval = 0 to prevent OkHttp client-side ping timeouts
    // when edge proxies buffer or delay pong responses. RFC 6455 server pings are still
    // automatically answered with pongs by OkHttp.
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive WebSocket stream
        .pingInterval(0, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null

    private var isManualDisconnect = false
    private var reconnectAttempts = 0
    private var lastUrl: String = ""
    private var lastToken: String = ""
    private var lastRoomName: String = "nola-agent-room"
    private var lastIdentity: String = "user-mobile"

    fun connect(
        url: String,
        token: String,
        roomName: String = "nola-agent-room",
        identity: String = "user-mobile"
    ) {
        isManualDisconnect = false
        reconnectAttempts = 0
        reconnectJob?.cancel()

        lastUrl = url
        lastToken = token
        lastRoomName = roomName
        lastIdentity = identity

        performConnect(url, token, roomName, identity)
    }

    private fun performConnect(
        url: String,
        token: String,
        roomName: String,
        identity: String
    ) {
        closeCurrentSocket()

        _connectionState.value = if (reconnectAttempts > 0) LiveKitConnectionState.Reconnecting else LiveKitConnectionState.Connecting
        onStateChanged(_connectionState.value)

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

        Log.d(TAG, "Connecting to LiveKit: $cleanUrl (Room: $roomName, Reconnect attempt: $reconnectAttempts)")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to LiveKit signaling channel successfully.")
                reconnectAttempts = 0
                reconnectJob?.cancel()
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
                if (!isManualDisconnect) {
                    handleUnexpectedDisconnect()
                } else {
                    scope.launch(Dispatchers.Main) {
                        _connectionState.value = LiveKitConnectionState.Disconnected
                        onStateChanged(LiveKitConnectionState.Disconnected)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "LiveKit WebSocket connection event: ${t.message}")
                if (!isManualDisconnect) {
                    handleUnexpectedDisconnect()
                } else {
                    scope.launch(Dispatchers.Main) {
                        _connectionState.value = LiveKitConnectionState.Disconnected
                        onStateChanged(LiveKitConnectionState.Disconnected)
                    }
                }
            }
        })
    }

    private fun handleUnexpectedDisconnect() {
        pingJob?.cancel()
        scope.launch(Dispatchers.Main) {
            _connectionState.value = LiveKitConnectionState.Reconnecting
            onStateChanged(LiveKitConnectionState.Reconnecting)
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        if (isManualDisconnect) return

        reconnectJob = scope.launch(Dispatchers.IO) {
            if (reconnectAttempts < 6) {
                val backoffMs = (1000L * (1 shl reconnectAttempts.coerceAtMost(3))).coerceIn(1000L, 8000L)
                reconnectAttempts++
                Log.d(TAG, "Scheduling LiveKit reconnect attempt #$reconnectAttempts in ${backoffMs}ms...")
                delay(backoffMs)
                if (!isManualDisconnect && lastUrl.isNotBlank() && lastToken.isNotBlank()) {
                    withContext(Dispatchers.Main) {
                        performConnect(lastUrl, lastToken, lastRoomName, lastIdentity)
                    }
                }
            } else {
                Log.w(TAG, "LiveKit reconnection paused after maximum attempts. User can retry via settings.")
                withContext(Dispatchers.Main) {
                    _connectionState.value = LiveKitConnectionState.Disconnected
                    onStateChanged(LiveKitConnectionState.Disconnected)
                }
            }
        }
    }

    private fun parseSignalMessage(text: String) {
        try {
            if (text.startsWith("{")) {
                val json = JSONObject(text)
                // Handle LiveKit Signal Responses: join, active speakers, data packets, pong
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
                delay(25000)
                try {
                    // LiveKit standard JSON signal ping request
                    val pingJson = JSONObject().apply {
                        put("ping", System.currentTimeMillis())
                    }
                    webSocket?.send(pingJson.toString())
                } catch (e: Exception) {
                    Log.d(TAG, "Heartbeat ping send omitted: ${e.message}")
                }
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

    private fun closeCurrentSocket() {
        pingJob?.cancel()
        pingJob = null
        try {
            webSocket?.close(1000, "Switching connection")
        } catch (_: Exception) {}
        webSocket = null
    }

    fun disconnect() {
        isManualDisconnect = true
        reconnectAttempts = 0
        reconnectJob?.cancel()
        reconnectJob = null
        closeCurrentSocket()
        _connectionState.value = LiveKitConnectionState.Disconnected
        onStateChanged(LiveKitConnectionState.Disconnected)
    }
}
