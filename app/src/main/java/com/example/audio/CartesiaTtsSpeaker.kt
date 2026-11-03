package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

private const val TAG = "NolaCartesiaTTS"

/**
 * Cartesia Sonic AI Text-To-Speech engine.
 * Synthesizes ultra-low latency, lifelike AI voice audio directly from Cartesia
 * (the voice provider used in LiveKit voice pipelines).
 * Falls back to AndroidTtsSpeaker if Cartesia key is unconfigured or network fails.
 */
class CartesiaTtsSpeaker(
    private val context: Context,
    private val scope: CoroutineScope,
    private val fallbackTts: AndroidTtsSpeaker,
    private var apiKey: String = "",
    private var voiceId: String = "79a125e8-cd45-4c13-8a67-188112f4dd22"
) {
    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var amplitudeJob: Job? = null
    private var currentAudioFile: File? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    fun updateConfig(apiKey: String, voiceId: String? = null) {
        this.apiKey = apiKey.trim()
        if (!voiceId.isNullOrBlank()) {
            this.voiceId = voiceId.trim()
        }
        Log.i(TAG, "[Diagnostic] Cartesia TTS config updated (API Key configured: ${this.apiKey.isNotBlank()}, Voice ID: ${this.voiceId})")
    }

    /**
     * Synthesizes and plays speech using Cartesia Sonic API.
     */
    fun speak(
        text: String,
        isSpeakerMuted: Boolean = false,
        onStart: () -> Unit = {},
        onAmplitude: (Float) -> Unit = {},
        onDone: () -> Unit = {}
    ) {
        stop()

        if (text.isBlank() || isSpeakerMuted) {
            onDone()
            return
        }

        // If Cartesia API key is not supplied, use fallback TTS engine
        if (apiKey.isBlank()) {
            Log.i(TAG, "[Diagnostic] No Cartesia API key provided. Using fallback Android TTS engine.")
            fallbackTts.speak(text, isSpeakerMuted, onStart, onAmplitude, onDone)
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "[Diagnostic] Requesting Cartesia TTS synthesis for ${text.length} chars...")

                val jsonBody = JSONObject().apply {
                    put("model_id", "sonic-english")
                    put("transcript", text)
                    put("voice", JSONObject().apply {
                        put("mode", "id")
                        put("id", voiceId)
                    })
                    put("output_format", JSONObject().apply {
                        put("container", "wav")
                        put("encoding", "pcm_s16le")
                        put("sample_rate", 44100)
                    })
                }

                val request = Request.Builder()
                    .url("https://api.cartesia.ai/tts/bytes")
                    .header("X-API-Key", apiKey)
                    .header("Cartesia-Version", "2024-06-10")
                    .header("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.w(TAG, "[Diagnostic] Cartesia TTS API returned ${response.code}: $errBody. Falling back to local TTS.")
                    withContext(Dispatchers.Main) {
                        fallbackTts.speak(text, isSpeakerMuted, onStart, onAmplitude, onDone)
                    }
                    return@launch
                }

                val bytes = response.body?.bytes()
                if (bytes == null || bytes.isEmpty()) {
                    Log.w(TAG, "[Diagnostic] Cartesia returned empty audio bytes. Falling back to local TTS.")
                    withContext(Dispatchers.Main) {
                        fallbackTts.speak(text, isSpeakerMuted, onStart, onAmplitude, onDone)
                    }
                    return@launch
                }

                Log.i(TAG, "[Diagnostic] Cartesia returned ${bytes.size} audio bytes successfully.")

                // Save to temporary cache WAV file
                val audioFile = File(context.cacheDir, "cartesia_speech_${System.currentTimeMillis()}.wav")
                FileOutputStream(audioFile).use { it.write(bytes) }
                currentAudioFile = audioFile

                withContext(Dispatchers.Main) {
                    playWavFile(audioFile, onStart, onAmplitude, onDone)
                }

            } catch (e: Exception) {
                Log.e(TAG, "[Diagnostic] Cartesia request exception: ${e.message}. Using fallback TTS.", e)
                withContext(Dispatchers.Main) {
                    fallbackTts.speak(text, isSpeakerMuted, onStart, onAmplitude, onDone)
                }
            }
        }
    }

    private fun playWavFile(
        file: File,
        onStart: () -> Unit,
        onAmplitude: (Float) -> Unit,
        onDone: () -> Unit
    ) {
        try {
            requestFocus()

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(file.absolutePath)
                prepare()

                setOnPreparedListener { mp ->
                    mp.start()
                    _isSpeaking.value = true
                    onStart()
                    startAmplitudeModulation(onAmplitude)
                    Log.i(TAG, "[Diagnostic] Cartesia audio playback started (Duration: ${mp.duration}ms)")
                }

                setOnCompletionListener {
                    Log.i(TAG, "[Diagnostic] Cartesia audio playback completed.")
                    cleanupPlayback()
                    onAmplitude(0.05f)
                    onDone()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "[Diagnostic] MediaPlayer playback error: what=$what, extra=$extra")
                    cleanupPlayback()
                    onAmplitude(0.05f)
                    onDone()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Diagnostic] Failed to start MediaPlayer: ${e.message}", e)
            cleanupPlayback()
            onDone()
        }
    }

    private fun startAmplitudeModulation(onAmplitude: (Float) -> Unit) {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch(Dispatchers.Default) {
            var step = 0f
            while (isActive && mediaPlayer?.isPlaying == true) {
                step += 0.25f
                val base = kotlin.math.sin(step.toDouble()).toFloat() * 0.38f + 0.58f
                val jitter = ((step.hashCode() % 12) / 100f)
                val amp = (base + jitter).coerceIn(0.2f, 0.95f)
                scope.launch(Dispatchers.Main) {
                    onAmplitude(amp)
                }
                delay(35)
            }
        }
    }

    private fun requestFocus() {
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .build()
            audioFocusRequest = req
            am.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (_: Exception) {}
    }

    private fun cleanupPlayback() {
        _isSpeaking.value = false
        amplitudeJob?.cancel()
        amplitudeJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        abandonFocus()

        try {
            currentAudioFile?.delete()
        } catch (_: Exception) {}
        currentAudioFile = null
    }

    fun stop() {
        cleanupPlayback()
        fallbackTts.stop()
    }

    fun shutdown() {
        stop()
        fallbackTts.shutdown()
    }
}
