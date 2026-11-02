package com.example.model

enum class AgentState(val label: String) {
    Disconnected("Disconnected"),
    Connecting("Connecting to Agent"),
    Connected("Connected"),
    Listening("Listening"),
    Thinking("Thinking..."),
    Speaking("Speaking")
}

enum class VisualizerType(val displayName: String, val description: String) {
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
    val serverUrl: String = "ws://192.168.1.100:7880",
    val roomName: String = "agent-room",
    val token: String = "",
    val tokenServerUrl: String = "http://192.168.1.100:3000/api/token",
    val isDemoMode: Boolean = true,
    val isSpeakerOn: Boolean = true,
    val visualizerType: VisualizerType = VisualizerType.Aura
)
