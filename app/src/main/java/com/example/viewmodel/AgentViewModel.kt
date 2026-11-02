package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiVoiceService
import com.example.audio.AndroidTtsSpeaker
import com.example.audio.AudioLevelAnalyzer
import com.example.data.AppDatabase
import com.example.data.SessionEntity
import com.example.data.SessionRepository
import com.example.livekit.LiveKitClientSession
import com.example.livekit.LiveKitConnectionState
import com.example.livekit.LiveKitTokenGenerator
import com.example.memory.Mem0Service
import com.example.memory.MemoryItem
import com.example.model.AgentState
import com.example.model.LiveKitConfig
import com.example.model.Speaker
import com.example.model.TranscriptEntry
import com.example.model.VisualizerType
import com.example.stt.SpeechRecognizerService
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
import java.util.UUID
import java.util.concurrent.TimeUnit

class AgentViewModel(application: Application) : AndroidViewModel(application) {

    private val audioAnalyzer = AudioLevelAnalyzer(viewModelScope)
    private val geminiService = GeminiVoiceService()
    val mem0Service = Mem0Service()
    val ttsSpeaker = AndroidTtsSpeaker(application.applicationContext, viewModelScope)
    private val sessionRepository = SessionRepository(AppDatabase.getDatabase(application).sessionDao())

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

    private val _isChatMode = MutableStateFlow(false)
    val isChatMode: StateFlow<Boolean> = _isChatMode.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _isMemorySheetOpen = MutableStateFlow(false)
    val isMemorySheetOpen: StateFlow<Boolean> = _isMemorySheetOpen.asStateFlow()

    private val _isHistoryOpen = MutableStateFlow(false)
    val isHistoryOpen: StateFlow<Boolean> = _isHistoryOpen.asStateFlow()

    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions: StateFlow<List<SessionEntity>> = _sessions.asStateFlow()

    private val _memories = MutableStateFlow<List<MemoryItem>>(emptyList())
    val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    private val _latencyMs = MutableStateFlow(24)
    val latencyMs: StateFlow<Int> = _latencyMs.asStateFlow()

    private val _config = MutableStateFlow(LiveKitConfig())
    val config: StateFlow<LiveKitConfig> = _config.asStateFlow()

    private val _selectedVisualizer = MutableStateFlow(VisualizerType.Aura)
    val selectedVisualizer: StateFlow<VisualizerType> = _selectedVisualizer.asStateFlow()

    private val _transcripts = MutableStateFlow<List<TranscriptEntry>>(emptyList())
    val transcripts: StateFlow<List<TranscriptEntry>> = _transcripts.asStateFlow()

    private val _statusMessage = MutableStateFlow("Connected to LiveKit Cloud")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val speechRecognizer = SpeechRecognizerService(
        context = application.applicationContext,
        scope = viewModelScope,
        speechmaticsApiKey = _config.value.speechmaticsApiKey
    ) { recognizedText ->
        processUserVoiceInput(recognizedText)
    }

    val sttProviderName: StateFlow<String> = speechRecognizer.sttProviderName
    val isSpeechmaticsCreditDepleted: StateFlow<Boolean> = speechRecognizer.isCreditDepleted

    // Official LiveKit Client Session connected to Cloud WebRTC Signal
    private val liveKitSession = LiveKitClientSession(
        scope = viewModelScope,
        onMessageReceived = { message ->
            handleLiveKitMessage(message)
        },
        onAgentSpeaking = { isSpeaking ->
            if (isSpeaking && _agentState.value != AgentState.Speaking) {
                _agentState.value = AgentState.Speaking
                _statusMessage.value = "Nola is speaking via LiveKit"
            } else if (!isSpeaking && _agentState.value == AgentState.Speaking) {
                _agentState.value = AgentState.Listening
                _statusMessage.value = "Listening to you"
            }
        },
        onStateChanged = { connectionState ->
            when (connectionState) {
                LiveKitConnectionState.Connected -> {
                    _statusMessage.value = "Connected to LiveKit Cloud (${_config.value.roomName})"
                    addTranscript(Speaker.System, "LiveKit Cloud room established: ${_config.value.roomName}")
                }
                LiveKitConnectionState.Connecting -> {
                    _statusMessage.value = "Connecting to LiveKit Cloud..."
                }
                LiveKitConnectionState.Reconnecting -> {
                    _statusMessage.value = "Reconnecting to LiveKit..."
                }
                LiveKitConnectionState.Disconnected -> {
                    _statusMessage.value = "Disconnected from LiveKit"
                }
            }
        }
    )

    val liveKitConnectionState: StateFlow<LiveKitConnectionState> = liveKitSession.connectionState

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

        // Collect sessions from local Room database
        viewModelScope.launch {
            sessionRepository.allSessions.collect { sessionList ->
                _sessions.value = sessionList
            }
        }

        // Add initial system greeting transcript
        addTranscript(
            speaker = Speaker.System,
            text = "Nola session ready. LiveKit Cloud: ${_config.value.serverUrl} (Room: ${_config.value.roomName})"
        )

        // Load initial memories from Mem0
        viewModelScope.launch {
            val initialMemories = mem0Service.getAllMemories()
            _memories.value = initialMemories
        }

        // Connect to LiveKit Cloud automatically
        connectToLiveKitCloud()

        // Start listening immediately
        startMicCapture()
    }

    private fun connectToLiveKitCloud() {
        val cfg = _config.value
        val tokenToUse = if (cfg.token.isNotBlank()) {
            cfg.token
        } else if (cfg.apiKey.isNotBlank() && cfg.apiSecret.isNotBlank()) {
            LiveKitTokenGenerator.createToken(
                apiKey = cfg.apiKey,
                apiSecret = cfg.apiSecret,
                roomName = cfg.roomName
            )
        } else {
            ""
        }

        if (tokenToUse.isNotBlank() && !cfg.isDemoMode) {
            _config.value = cfg.copy(token = tokenToUse)
            liveKitSession.connect(
                url = cfg.serverUrl,
                token = tokenToUse,
                roomName = cfg.roomName
            )
        }
    }

    fun startMicCapture() {
        if (!_isMicMuted.value) {
            audioAnalyzer.startListening()
            speechRecognizer.startListening()
        }
    }

    fun stopMicCapture() {
        audioAnalyzer.stopListening()
        speechRecognizer.stopListening()
    }

    fun toggleMic() {
        val newMuted = !_isMicMuted.value
        _isMicMuted.value = newMuted
        if (newMuted) {
            audioAnalyzer.stopListening()
            speechRecognizer.stopListening()
            _audioAmplitude.value = 0.02f
            if (_agentState.value == AgentState.Listening) {
                _statusMessage.value = "Microphone muted"
            }
        } else {
            audioAnalyzer.startListening()
            speechRecognizer.startListening()
            if (_agentState.value == AgentState.Listening) {
                _statusMessage.value = "Listening to you"
            }
        }
    }

    fun toggleSpeaker() {
        val newState = !_isSpeakerOn.value
        _isSpeakerOn.value = newState
        if (!newState) {
            ttsSpeaker.stop()
        }
    }

    fun toggleTranscript() {
        _isTranscriptVisible.value = !_isTranscriptVisible.value
    }

    fun toggleChatMode() {
        _isChatMode.value = !_isChatMode.value
    }

    fun setChatMode(enabled: Boolean) {
        _isChatMode.value = enabled
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun openMemorySheet() {
        _isMemorySheetOpen.value = true
        viewModelScope.launch {
            _memories.value = mem0Service.getAllMemories()
        }
    }

    fun closeMemorySheet() {
        _isMemorySheetOpen.value = false
    }

    fun openHistory() {
        _isHistoryOpen.value = true
    }

    fun closeHistory() {
        _isHistoryOpen.value = false
    }

    fun startNewSession() {
        val newId = UUID.randomUUID().toString()
        _currentSessionId.value = newId
        _transcripts.value = emptyList()
        addTranscript(
            speaker = Speaker.System,
            text = "Started new voice session."
        )
        closeHistory()
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _currentSessionId.value = sessionId
            val messages = sessionRepository.getMessagesForSessionSync(sessionId)
            val loadedTranscripts = messages.map { msg ->
                val spk = when (msg.speaker) {
                    "User" -> Speaker.User
                    "Agent" -> Speaker.Agent
                    else -> Speaker.System
                }
                TranscriptEntry(
                    id = msg.id,
                    speaker = spk,
                    text = msg.text,
                    timestamp = msg.timestamp
                )
            }
            _transcripts.value = loadedTranscripts
            closeHistory()
            _isChatMode.value = true
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                startNewSession()
            }
        }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            sessionRepository.clearAll()
            startNewSession()
        }
    }

    fun setVisualizer(type: VisualizerType) {
        _selectedVisualizer.value = type
        _config.value = _config.value.copy(visualizerType = type)
    }

    fun updateConfig(newConfig: LiveKitConfig) {
        _config.value = newConfig
        _selectedVisualizer.value = newConfig.visualizerType
        speechRecognizer.setApiKey(newConfig.speechmaticsApiKey)
        mem0Service.setApiKey(newConfig.mem0ApiKey)

        // Reconnect to LiveKit Cloud with updated parameters
        connectToLiveKitCloud()
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
                        val fetchedToken = body.trim().replace("\"", "")
                        _config.value = _config.value.copy(token = fetchedToken)
                        addTranscript(Speaker.System, "Fetched new LiveKit token from server.")
                        connectToLiveKitCloud()
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
     * Processes live user voice input using Mem0 memory search, Gemini, LiveKit data channel,
     * and speaks the result out loud via Android TextToSpeech.
     */
    fun processUserVoiceInput(spokenText: String) {
        if (spokenText.isBlank() || _agentState.value == AgentState.Speaking) return

        viewModelScope.launch {
            // Stop listening while agent processes and speaks
            speechRecognizer.stopListening()

            // 1. Add user message
            addTranscript(Speaker.User, spokenText)
            _statusMessage.value = "Consulting Mem0 memory..."

            // Send to LiveKit session data channel
            liveKitSession.sendData(spokenText)

            // 2. Transition to Thinking state
            _agentState.value = AgentState.Thinking
            _audioAmplitude.value = 0.45f

            // 3. Search relevant memories in Mem0
            val relevantMemories = try {
                mem0Service.searchMemories(spokenText)
            } catch (_: Exception) {
                emptyList()
            }

            _statusMessage.value = "Nola thinking with Gemini..."

            // 4. Generate intelligent spoken response using Firebase AI Logic Gemini
            val recentTurns = _transcripts.value.takeLast(6).map {
                when (it.speaker) {
                    Speaker.User -> "User" to it.text
                    Speaker.Agent -> "Nola" to it.text
                    Speaker.System -> "System" to it.text
                }
            }

            val geminiResponse = try {
                geminiService.generateVoiceResponse(
                    userPrompt = spokenText,
                    relevantMemories = relevantMemories,
                    recentTranscripts = recentTurns
                )
            } catch (e: Exception) {
                "I hear you clearly, and I am ready with the next step."
            }

            // 5. Add Nola response to transcript and speak out loud
            addTranscript(Speaker.Agent, geminiResponse)
            triggerAgentSpeaking(geminiResponse)

            // 6. Asynchronously save turn into Mem0 memory so Nola remembers next time
            launch(Dispatchers.IO) {
                try {
                    mem0Service.addMemory(spokenText, geminiResponse)
                    val updated = mem0Service.getAllMemories()
                    _memories.value = updated
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Sends a text message from the chat view.
     */
    fun sendTextMessage(messageText: String) {
        processUserVoiceInput(messageText)
    }

    /**
     * Replays or speaks any text message out loud on demand.
     */
    fun speakTextAloud(text: String) {
        triggerAgentSpeaking(text)
    }

    private fun handleLiveKitMessage(message: String) {
        addTranscript(Speaker.Agent, message)
        triggerAgentSpeaking(message)
    }

    /**
     * Synthesizes agent speech out loud using Android TextToSpeech
     * and synchronizes amplitude with the visualizer.
     */
    private fun triggerAgentSpeaking(response: String) {
        _agentState.value = AgentState.Speaking
        _statusMessage.value = "Nola is speaking"

        ttsSpeaker.speak(
            text = response,
            isSpeakerMuted = !_isSpeakerOn.value,
            onStart = {
                _agentState.value = AgentState.Speaking
                _statusMessage.value = "Nola is speaking"
            },
            onAmplitude = { amp ->
                _audioAmplitude.value = amp
            },
            onDone = {
                _audioAmplitude.value = 0.05f
                _agentState.value = AgentState.Listening
                _statusMessage.value = "Listening to you"
                startMicCapture()
            }
        )
    }

    fun endSession() {
        ttsSpeaker.stop()
        speechRecognizer.stopListening()
        liveKitSession.disconnect()
        _agentState.value = AgentState.Disconnected
        _statusMessage.value = "Session ended. Tap to reconnect."
        _audioAmplitude.value = 0.01f
        addTranscript(Speaker.System, "Session disconnected.")
    }

    fun reconnectSession() {
        _agentState.value = AgentState.Connecting
        _statusMessage.value = "Reconnecting to LiveKit Cloud..."
        viewModelScope.launch {
            delay(900)
            connectToLiveKitCloud()
            _agentState.value = AgentState.Listening
            _statusMessage.value = "Connected and listening"
            addTranscript(Speaker.System, "Reconnected to Nola voice session.")
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

        // Persist message in Room database
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sessionRepository.saveMessage(
                    sessionId = _currentSessionId.value,
                    speaker = speaker.name,
                    text = text
                )
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioAnalyzer.stopListening()
        ttsSpeaker.shutdown()
        speechRecognizer.destroy()
        liveKitSession.disconnect()
    }
}
