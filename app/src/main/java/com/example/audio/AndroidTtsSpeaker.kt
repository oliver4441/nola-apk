package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import java.util.Locale
import java.util.UUID

private const val TAG = "NolaAudioService"

data class AudioDiagnostics(
    val isTtsInitialized: Boolean = false,
    val ttsEngine: String = "Unknown",
    val streamVolume: Int = 0,
    val maxVolume: Int = 0,
    val isStreamMuted: Boolean = false,
    val audioFocusGranted: Boolean = false,
    val lastError: String? = null
)

/**
 * Built-in Android Text-To-Speech engine for Nola with explicit AudioFocus tracking,
 * stream initialization verification, and comprehensive diagnostics.
 */
class AndroidTtsSpeaker(
    private val context: Context,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var speechAnimationJob: Job? = null
    private var currentUtteranceId: String? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _diagnostics = MutableStateFlow(AudioDiagnostics())
    val diagnostics: StateFlow<AudioDiagnostics> = _diagnostics.asStateFlow()

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    init {
        Log.i(TAG, "[Diagnostic] Initializing AndroidTtsSpeaker with AudioManager...")
        runDiagnosticCheck("Pre-Init")
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "[Diagnostic] Failed to instantiate TextToSpeech: ${e.message}", e)
            _diagnostics.value = _diagnostics.value.copy(lastError = e.message)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                tts?.setAudioAttributes(audioAttributes)
            } catch (e: Exception) {
                Log.w(TAG, "[Diagnostic] Setting AudioAttributes warning: ${e.message}")
            }

            val langResult = tts?.setLanguage(Locale.US)
            val langAvailable = langResult != TextToSpeech.LANG_MISSING_DATA && langResult != TextToSpeech.LANG_NOT_SUPPORTED
            if (!langAvailable) {
                tts?.language = Locale.getDefault()
            }

            tts?.setPitch(1.05f)
            tts?.setSpeechRate(1.02f)
            isInitialized = true

            val engineName = tts?.defaultEngine ?: "Default"
            Log.i(TAG, "[Diagnostic] TextToSpeech successfully initialized. Engine: $engineName, Language Available: $langAvailable")

            runDiagnosticCheck("Post-Init")
        } else {
            Log.e(TAG, "[Diagnostic] TextToSpeech initialization failed with code: $status")
            _diagnostics.value = _diagnostics.value.copy(lastError = "TTS Init failed with code $status")
        }
    }

    /**
     * Inspects media volume, mute state, ringer mode, and audio focus.
     */
    fun runDiagnosticCheck(callerTag: String): AudioDiagnostics {
        val am = audioManager
        val curVol = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val maxVol = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val isMuted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am?.isStreamMute(AudioManager.STREAM_MUSIC) == true
        } else {
            curVol == 0
        }
        val ringer = am?.ringerMode ?: -1

        val diag = AudioDiagnostics(
            isTtsInitialized = isInitialized,
            ttsEngine = tts?.defaultEngine ?: "Android",
            streamVolume = curVol,
            maxVolume = maxVol,
            isStreamMuted = isMuted,
            audioFocusGranted = _diagnostics.value.audioFocusGranted,
            lastError = _diagnostics.value.lastError
        )
        _diagnostics.value = diag

        Log.i(
            TAG,
            "[$callerTag Diagnostic] Output Stream: STREAM_MUSIC=$curVol/$maxVol, muted=$isMuted, ringerMode=$ringer, ttsReady=$isInitialized"
        )
        if (isMuted || curVol == 0) {
            Log.w(TAG, "[$callerTag Diagnostic Warning] Device STREAM_MUSIC is muted or at volume 0. Audio may not be audible!")
        }
        return diag
    }

    private fun requestFocus(): Boolean {
        val am = audioManager ?: return false
        val granted: Boolean

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { focusChange ->
                    Log.i(TAG, "[Diagnostic] AudioFocus change event: $focusChange")
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        Log.w(TAG, "[Diagnostic] Lost audio focus ($focusChange)")
                    }
                }
                .build()
            audioFocusRequest = req
            val res = am.requestAudioFocus(req)
            granted = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            Log.i(TAG, "[Diagnostic] AudioFocusRequest (API 26+) result: $res, granted: $granted")
        } else {
            @Suppress("DEPRECATION")
            val res = am.requestAudioFocus(
                { focusChange -> Log.i(TAG, "[Diagnostic] AudioFocus change event: $focusChange") },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            granted = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            Log.i(TAG, "[Diagnostic] requestAudioFocus (Legacy) result: $res, granted: $granted")
        }

        _diagnostics.value = _diagnostics.value.copy(audioFocusGranted = granted)
        return granted
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
            _diagnostics.value = _diagnostics.value.copy(audioFocusGranted = false)
            Log.i(TAG, "[Diagnostic] AudioFocus abandoned cleanly.")
        } catch (e: Exception) {
            Log.w(TAG, "[Diagnostic] Error abandoning audio focus: ${e.message}")
        }
    }

    /**
     * Synthesizes and plays Gemini speech output out loud through STREAM_MUSIC.
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
            Log.i(TAG, "[Diagnostic] Speak skipped: text blank (${text.isBlank()}) or muted ($isSpeakerMuted)")
            onDone()
            return
        }

        runDiagnosticCheck("SpeakRequest")
        val focusGranted = requestFocus()
        if (!focusGranted) {
            Log.w(TAG, "[Diagnostic] AudioFocus was not granted by system; proceeding with output attempt.")
        }

        val utteranceId = UUID.randomUUID().toString()
        currentUtteranceId = utteranceId

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceIdParam: String?) {
                if (utteranceIdParam == utteranceId) {
                    Log.i(TAG, "[Diagnostic] Utterance started playback: $utteranceId")
                    scope.launch(Dispatchers.Main) {
                        onStart()
                    }
                    startAmplitudeModulation(onAmplitude)
                }
            }

            override fun onDone(utteranceIdParam: String?) {
                if (utteranceIdParam == utteranceId) {
                    Log.i(TAG, "[Diagnostic] Utterance completed playback successfully: $utteranceId")
                    stopAmplitudeModulation()
                    abandonFocus()
                    scope.launch(Dispatchers.Main) {
                        onAmplitude(0.05f)
                        onDone()
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceIdParam: String?) {
                if (utteranceIdParam == utteranceId) {
                    Log.e(TAG, "[Diagnostic] Utterance failed: $utteranceId")
                    stopAmplitudeModulation()
                    abandonFocus()
                    scope.launch(Dispatchers.Main) {
                        onAmplitude(0.05f)
                        onDone()
                    }
                }
            }

            override fun onError(utteranceIdParam: String?, errorCode: Int) {
                Log.e(TAG, "[Diagnostic] Utterance error code $errorCode for ID: $utteranceId")
                if (utteranceIdParam == utteranceId) {
                    stopAmplitudeModulation()
                    abandonFocus()
                    scope.launch(Dispatchers.Main) {
                        onAmplitude(0.05f)
                        onDone()
                    }
                }
            }
        })

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }

        val speakResult = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        Log.i(TAG, "[Diagnostic] tts.speak invoked for utterance $utteranceId with result code: $speakResult")

        if (speakResult != TextToSpeech.SUCCESS) {
            Log.w(TAG, "[Diagnostic] TTS speak returned non-success code $speakResult. Initiating fallback playback simulation.")
            runSimulationFallback(text, onStart, onAmplitude, onDone)
        }
    }

    private fun startAmplitudeModulation(onAmplitude: (Float) -> Unit) {
        speechAnimationJob?.cancel()
        speechAnimationJob = scope.launch(Dispatchers.Default) {
            var step = 0f
            while (isActive) {
                step += 0.22f
                val base = kotlin.math.sin(step.toDouble()).toFloat() * 0.35f + 0.55f
                val jitter = ((step.hashCode() % 15) / 100f)
                val amp = (base + jitter).coerceIn(0.25f, 0.95f)
                scope.launch(Dispatchers.Main) {
                    onAmplitude(amp)
                }
                delay(40)
            }
        }
    }

    private fun stopAmplitudeModulation() {
        speechAnimationJob?.cancel()
        speechAnimationJob = null
    }

    private fun runSimulationFallback(
        text: String,
        onStart: () -> Unit,
        onAmplitude: (Float) -> Unit,
        onDone: () -> Unit
    ) {
        scope.launch(Dispatchers.Main) {
            onStart()
            startAmplitudeModulation(onAmplitude)
            val durationMs = (text.length * 55L).coerceIn(1800L, 6500L)
            delay(durationMs)
            stopAmplitudeModulation()
            abandonFocus()
            onAmplitude(0.05f)
            onDone()
        }
    }

    fun stop() {
        stopAmplitudeModulation()
        try {
            tts?.stop()
        } catch (_: Exception) {}
        abandonFocus()
        currentUtteranceId = null
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
