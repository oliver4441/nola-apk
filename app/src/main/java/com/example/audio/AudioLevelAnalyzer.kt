package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Real-time microphone audio amplitude analyzer.
 * Captures mic audio buffers and converts them to normalized amplitude levels (0.0 to 1.0)
 * with low-pass exponential smoothing for 60fps wave visualizer reactivity.
 */
class AudioLevelAnalyzer(
    private val scope: CoroutineScope
) {
    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isAnalyzing = false

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isAnalyzing) return
        try {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                fallbackSimulation()
                return
            }

            val bufferSize = minBufferSize.coerceAtLeast(2048)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                fallbackSimulation()
                return
            }

            audioRecord?.startRecording()
            isAnalyzing = true

            recordingJob = scope.launch(Dispatchers.Default) {
                val buffer = ShortArray(bufferSize / 2)
                var smoothed = 0f
                while (isActive && isAnalyzing) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readCount > 0) {
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / readCount)
                        // Normalize 16-bit PCM (max 32767) to [0.0, 1.0]
                        val rawLevel = (rms / 32767.0 * 6.0).toFloat().coerceIn(0f, 1f)
                        // Exponential moving average for fluid responsiveness
                        smoothed = smoothed * 0.65f + rawLevel * 0.35f
                        _amplitude.value = smoothed
                    }
                    delay(16) // ~60fps
                }
            }
        } catch (_: Exception) {
            fallbackSimulation()
        }
    }

    private fun fallbackSimulation() {
        isAnalyzing = true
        recordingJob = scope.launch(Dispatchers.Default) {
            var phase = 0f
            while (isActive && isAnalyzing) {
                phase += 0.1f
                val sim = (kotlin.math.sin(phase.toDouble()).toFloat() * 0.5f + 0.5f) * 0.25f
                _amplitude.value = sim
                delay(32)
            }
        }
    }

    fun stopListening() {
        isAnalyzing = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {
            // Ignore clean teardown errors
        } finally {
            audioRecord = null
            _amplitude.value = 0f
        }
    }

    fun setSimulatedAmplitude(level: Float) {
        _amplitude.value = level.coerceIn(0f, 1f)
    }
}
