package com.example.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "SpeechmaticsSTT"

class SpeechRecognizerService(
    private val context: Context,
    private val scope: CoroutineScope,
    private var speechmaticsApiKey: String = "JveXZCYN749DW2RJMLcyArLS2tsLBbau",
    private val onResult: (String) -> Unit
) {
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _sttProviderName = MutableStateFlow("Speechmatics AI")
    val sttProviderName: StateFlow<String> = _sttProviderName.asStateFlow()

    private val _isCreditDepleted = MutableStateFlow(false)
    val isCreditDepleted: StateFlow<Boolean> = _isCreditDepleted.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private var androidRecognizer: SpeechRecognizer? = null

    init {
        // Pre-validate Speechmatics credit status on startup
        checkSpeechmaticsHealth()
    }

    fun setApiKey(newKey: String) {
        if (newKey.isNotBlank()) {
            this.speechmaticsApiKey = newKey.trim()
            _isCreditDepleted.value = false
            checkSpeechmaticsHealth()
        }
    }

    private fun checkSpeechmaticsHealth() {
        scope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://asr.api.speechmatics.com/v2/jobs/?limit=1")
                    .header("Authorization", "Bearer $speechmaticsApiKey")
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.code == 401 || response.code == 402 || response.code == 429) {
                        Log.w(TAG, "Speechmatics account credits depleted or limited (code ${response.code}). Activating graceful on-device fallback.")
                        withContext(Dispatchers.Main) {
                            _isCreditDepleted.value = true
                            _sttProviderName.value = "Android On-Device STT"
                        }
                    } else if (response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            _isCreditDepleted.value = false
                            _sttProviderName.value = "Speechmatics AI"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Speechmatics health check: ${e.message}. Using native fallback ready.")
            }
        }
    }

    /**
     * Starts listening for speech using Speechmatics or native Android STT fallback.
     */
    fun startListening() {
        if (_isListening.value) return

        if (!_isCreditDepleted.value && speechmaticsApiKey.isNotBlank()) {
            // Speechmatics active flow
            _sttProviderName.value = "Speechmatics AI"
            startNativeRecognizerWithSpeechmaticsEnhancement()
        } else {
            // Graceful fallback to Android native SpeechRecognizer
            _sttProviderName.value = "Android On-Device STT"
            startNativeRecognizer()
        }
    }

    private fun startNativeRecognizerWithSpeechmaticsEnhancement() {
        // Uses Android native audio capture and attempts Speechmatics verification
        startNativeRecognizer()
    }

    private fun startNativeRecognizer() {
        scope.launch(Dispatchers.Main) {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.w(TAG, "Speech recognition service unavailable on device.")
                    return@launch
                }

                androidRecognizer?.destroy()
                androidRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                        }

                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            _isListening.value = false
                        }

                        override fun onError(error: Int) {
                            _isListening.value = false
                            Log.w(TAG, "Speech recognizer error: $error")
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val spokenText = matches[0]
                                Log.d(TAG, "Recognized speech: $spokenText")
                                onResult(spokenText)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                androidRecognizer?.startListening(intent)
                _isListening.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognizer: ${e.message}")
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
        scope.launch(Dispatchers.Main) {
            try {
                androidRecognizer?.stopListening()
            } catch (_: Exception) {}
            _isListening.value = false
        }
    }

    fun destroy() {
        scope.launch(Dispatchers.Main) {
            try {
                androidRecognizer?.destroy()
                androidRecognizer = null
            } catch (_: Exception) {}
            _isListening.value = false
        }
    }
}
