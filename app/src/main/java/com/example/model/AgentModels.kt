package com.example.model

enum class AgentState(val label: String) {
    Disconnected("Disconnected"),
    Connecting("Connecting"),
    Connected("Connected"),
    Listening("Listening"),
    Thinking("Thinking"),
    Speaking("Speaking")
}

enum class VisualizerType(val displayName: String, val description: String) {
    Avatar("Nola", "Dimensional avatar with a voice-reactive expression"),
    Aura("Aura Orb", "Gemini Live fluid glowing aura orb"),
    Radial("Radial", "Circular frequency spectrum ring"),
    Bar("Equalizer", "Vertical multi-band audio bars"),
    Wave("Wave", "Oscilloscope multi-harmonic wave")
}

enum class Speaker {
    User,
    Agent,
    System
}

data class TranscriptEntry(
    val id: String,
    val speaker: Speaker,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFinal: Boolean = true
)

data class LiveKitConfig(
    val serverUrl: String = "wss://nola-nr9t1jj5.livekit.cloud",
    val roomName: String = "nola-agent-room",
    val apiKey: String = "API6KvEtLuf2muJ",
    val apiSecret: String = "7IUG92S4tr4AnjRYp3at3MJztfwtBePam5hm9ZnM9z9",
    val token: String = "",
    val tokenServerUrl: String = "",
    val mem0ApiKey: String = "m0-8DbLHr53LfqrdtcIgTFq8VTOSFZh4HIzSMYpLN1y",
    val cartesiaApiKey: String = "",
    val cartesiaVoiceId: String = "79a125e8-cd45-4c13-8a67-188112f4dd22",
    val isDemoMode: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val visualizerType: VisualizerType = VisualizerType.Avatar
)
