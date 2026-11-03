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
import java.util.Locale

private const val TAG = "NolaSpeechRecognizer"

/**
 * Clean on-device Android SpeechRecognizer service for voice-to-text.
 * Speechmatics has been completely removed as requested.
 */
class SpeechRecognizerService(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onResult: (String) -> Unit
) {
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private var androidRecognizer: SpeechRecognizer? = null

    init {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        Log.i(TAG, "[Diagnostic] Android SpeechRecognizer available: $available")
    }

    /**
     * Starts listening for speech using Android's on-device SpeechRecognizer.
     */
    fun startListening() {
        if (_isListening.value) return

        scope.launch(Dispatchers.Main) {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.w(TAG, "[Diagnostic] Speech recognition service not available on this device.")
                    return@launch
                }

                androidRecognizer?.destroy()
                androidRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                            Log.d(TAG, "[Diagnostic] SpeechRecognizer ready for speech.")
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d(TAG, "[Diagnostic] User started speaking.")
                        }

                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _isListening.value = false
                            Log.d(TAG, "[Diagnostic] User finished speaking.")
                        }

                        override fun onError(error: Int) {
                            _isListening.value = false
                            Log.w(TAG, "[Diagnostic] Speech recognizer error code: $error")
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val spokenText = matches[0]
                                Log.i(TAG, "[Diagnostic] Speech recognized: $spokenText")
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
                Log.i(TAG, "[Diagnostic] SpeechRecognizer started listening.")
            } catch (e: Exception) {
                Log.e(TAG, "[Diagnostic] Failed to start speech recognizer: ${e.message}", e)
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
