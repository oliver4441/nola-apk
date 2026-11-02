package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioLevelAnalyzer
import com.example.model.AgentState
import com.example.model.LiveKitConfig
import com.example.model.Speaker
import com.example.model.TranscriptEntry
import com.example.model.VisualizerType
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
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class AgentViewModel(application: Application) : AndroidViewModel(application) {

    private val audioAnalyzer = AudioLevelAnalyzer(viewModelScope)

    private val _agentState = MutableStateFlow(AgentState.Listening)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0.05f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isTranscriptVisible = MutableStateFlow(false)
    val isTranscriptVisible: StateFlow<Boolean> = _isTranscriptVisible.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _latencyMs = MutableStateFlow(24)
    val latencyMs: StateFlow<Int> = _latencyMs.asStateFlow()

    private val _config = MutableStateFlow(LiveKitConfig())
    val config: StateFlow<LiveKitConfig> = _config.asStateFlow()

    private val _selectedVisualizer = MutableStateFlow(VisualizerType.Aura)
    val selectedVisualizer: StateFlow<VisualizerType> = _selectedVisualizer.asStateFlow()

    private val _transcripts = MutableStateFlow<List<TranscriptEntry>>(emptyList())
    val transcripts: StateFlow<List<TranscriptEntry>> = _transcripts.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready to chat")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var speakingJob: Job? = null
    private var webSocket: WebSocket? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    init {
        // Collect real microphone audio amplitude when listening
        viewModelScope.launch {
            audioAnalyzer.amplitude.collect { micLevel ->
                if (_agentState.value == AgentState.Listening && !_isMicMuted.value) {
                    _audioAmplitude.value = micLevel
                }
            }
        }

        // Add initial system greeting transcript
        addTranscript(
            speaker = Speaker.System,
            text = "Connected to Nola voice agent session. Tap mic or speak naturally."
        )

        // Start listening immediately
        startMicCapture()
    }

    fun startMicCapture() {
        if (!_isMicMuted.value) {
            audioAnalyzer.startListening()
        }
    }

    fun stopMicCapture() {
        audioAnalyzer.stopListening()
    }

    fun toggleMic() {
        val newMuted = !_isMicMuted.value
        _isMicMuted.value = newMuted
        if (newMuted) {
            audioAnalyzer.stopListening()
            _audioAmplitude.value = 0.02f
            if (_agentState.value == AgentState.Listening) {
                _statusMessage.value = "Microphone muted"
            }
        } else {
            audioAnalyzer.startListening()
            if (_agentState.value == AgentState.Listening) {
                _statusMessage.value = "Listening to you"
            }
        }
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun toggleTranscript() {
        _isTranscriptVisible.value = !_isTranscriptVisible.value
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun setVisualizer(type: VisualizerType) {
        _selectedVisualizer.value = type
        _config.value = _config.value.copy(visualizerType = type)
    }

    fun updateConfig(newConfig: LiveKitConfig) {
        _config.value = newConfig
        _selectedVisualizer.value = newConfig.visualizerType
        if (!newConfig.isDemoMode && newConfig.serverUrl.isNotBlank()) {
            connectLiveKitWebSocket(newConfig.serverUrl, newConfig.token)
        }
    }

    fun fetchToken(endpointUrl: String) {
        if (endpointUrl.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(endpointUrl).build()
                val response: Response = httpClient.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotBlank()) {
                    withContext(Dispatchers.Main) {
                        _config.value = _config.value.copy(token = body.trim().replace("\"", ""))
                        addTranscript(Speaker.System, "Fetched new LiveKit token from server.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addTranscript(Speaker.System, "Failed to fetch token: ${e.message}")
                }
            }
        }
    }

    /**
     * Connects to LiveKit WebSocket signal server when configured.
     */
    private fun connectLiveKitWebSocket(serverUrl: String, token: String) {
        webSocket?.close(1000, "Reconnecting")
        _agentState.value = AgentState.Connecting
        _statusMessage.value = "Handshaking with LiveKit..."

        val wsUrl = if (token.isNotBlank()) {
            val separator = if (serverUrl.contains("?")) "&" else "?"
            "$serverUrl${separator}access_token=$token"
        } else {
            serverUrl
        }

        try {
            val request = Request.Builder().url(wsUrl).build()
            webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    viewModelScope.launch {
                        _agentState.value = AgentState.Connected
                        delay(600)
                        _agentState.value = AgentState.Listening
                        _statusMessage.value = "Agent connected and listening"
                        addTranscript(Speaker.System, "LiveKit room session established.")
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    viewModelScope.launch {
                        handleAgentMessage(text)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    viewModelScope.launch {
                        _statusMessage.value = "Connection dropped. Falling back to voice loop."
                        _agentState.value = AgentState.Listening
                    }
                }
            })
        } catch (e: Exception) {
            _statusMessage.value = "Connection error: ${e.message}"
            _agentState.value = AgentState.Listening
        }
    }

    private fun handleAgentMessage(message: String) {
        // Handle incoming data channel transcript or agent state
        addTranscript(Speaker.Agent, message)
        triggerAgentSpeaking(message)
    }

    /**
     * User voice prompt simulation or trigger.
     */
    fun simulateUserPrompt(promptText: String = "What are the latest updates on the task?") {
        if (_agentState.value == AgentState.Speaking) return

        viewModelScope.launch {
            // 1. User speaks
            addTranscript(Speaker.User, promptText)
            _statusMessage.value = "Analyzing speech..."

            // 2. Agent enters thinking state
            _agentState.value = AgentState.Thinking
            _audioAmplitude.value = 0.45f
            delay(1200)

            // 3. Agent responds
            val agentResponses = listOf(
                "I have completed running the tests and the LiveKit agent pipeline is active on your machine.",
                "All systems are synced. Audio tracks are transmitting at low latency.",
                "I am monitoring the server telemetry. Memory and CPU usage on your PC look steady.",
                "Understood. I will execute the routine and stream the output back to your device."
            )
            val selectedResponse = agentResponses[Random.nextInt(agentResponses.size)]
            triggerAgentSpeaking(selectedResponse)
        }
    }

    /**
     * Animates waveform dynamically while the agent is speaking.
     */
    private fun triggerAgentSpeaking(response: String) {
        speakingJob?.cancel()
        speakingJob = viewModelScope.launch {
            _agentState.value = AgentState.Speaking
            _statusMessage.value = "Nola is speaking"
            addTranscript(Speaker.Agent, response)

            val durationMs = (response.length * 55).coerceIn(2400, 7500)
            val startTime = System.currentTimeMillis()

            while (isActive && (System.currentTimeMillis() - startTime) < durationMs) {
                // Modulate amplitude rhythmically to match voice cadence
                val elapsed = (System.currentTimeMillis() - startTime).toFloat() / 200f
                val voiceAmp = (kotlin.math.sin(elapsed.toDouble()).toFloat() * 0.35f + 0.55f).coerceIn(0.2f, 0.95f)
                _audioAmplitude.value = voiceAmp
                delay(30)
            }

            // Return to listening
            _audioAmplitude.value = 0.05f
            _agentState.value = AgentState.Listening
            _statusMessage.value = "Listening to you"
        }
    }

    fun endSession() {
        speakingJob?.cancel()
        webSocket?.close(1000, "User ended call")
        webSocket = null
        _agentState.value = AgentState.Disconnected
        _statusMessage.value = "Session ended. Tap to reconnect."
        _audioAmplitude.value = 0.01f
        addTranscript(Speaker.System, "Session disconnected.")
    }

    fun reconnectSession() {
        _agentState.value = AgentState.Connecting
        _statusMessage.value = "Reconnecting to LiveKit..."
        viewModelScope.launch {
            delay(900)
            _agentState.value = AgentState.Listening
            _statusMessage.value = "Connected and listening"
            addTranscript(Speaker.System, "Reconnected to agent session.")
            startMicCapture()
        }
    }

    private fun addTranscript(speaker: Speaker, text: String) {
        val entry = TranscriptEntry(
            id = UUID.randomUUID().toString(),
            speaker = speaker,
            text = text
        )
        _transcripts.value = _transcripts.value + entry
    }

    override fun onCleared() {
        super.onCleared()
        audioAnalyzer.stopListening()
        webSocket?.close(1000, "App destroyed")
    }
}
