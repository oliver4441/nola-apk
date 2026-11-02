package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AgentState
import com.example.ui.theme.LiveKitCyan
import com.example.ui.theme.LiveKitIndigo
import com.example.ui.theme.LiveKitTeal
import com.example.ui.theme.StatusThinking
import kotlin.math.PI
import kotlin.math.sin

/**
 * LiveKit Agents UI Wave Visualizer Component.
 * Port of the LiveKit @agents-ui/agent-audio-visualizer-wave component to native Jetpack Compose.
 *
 * Implements:
 * - Real-time multi-harmonic oscilloscope animated wave
 * - Dynamic color shifting between cyan, indigo, and teal
 * - Responsive amplitude modulation according to audio volume and agent state
 * - Hann window edge tapering so the wave smoothly zeroes at the container edges
 * - Soft luminous ambient glow
 */
@Composable
fun AgentAudioVisualizerWave(
    state: AgentState,
    audioAmplitude: Float,
    modifier: Modifier = Modifier,
    height: Dp = 260.dp,
    primaryColor: Color = LiveKitCyan,
    secondaryColor: Color = LiveKitIndigo,
    accentColor: Color = LiveKitTeal,
    lineWidth: Dp = 3.dp,
    colorShift: Float = 0.35f
) {
    // Continuous infinite phase transition for wave motion
    val infiniteTransition = rememberInfiniteTransition(label = "WavePhaseTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AgentState.Thinking -> 1600
                    AgentState.Speaking -> 2200
                    AgentState.Listening -> 2800
                    AgentState.Connecting -> 3500
                    AgentState.Disconnected -> 6000
                    AgentState.Connected -> 3200
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    // Smooth animated amplitude envelope for responsiveness without jitter
    val smoothedAmplitude = remember { Animatable(0.05f) }

    LaunchedEffect(audioAmplitude, state) {
        val targetAmp = when (state) {
            AgentState.Disconnected -> 0.03f
            AgentState.Connecting -> 0.12f
            AgentState.Connected -> 0.08f
            AgentState.Listening -> (audioAmplitude * 0.85f + 0.08f).coerceIn(0.08f, 1.0f)
            AgentState.Thinking -> 0.35f
            AgentState.Speaking -> (audioAmplitude * 0.95f + 0.25f).coerceIn(0.25f, 1.0f)
        }
        smoothedAmplitude.animateTo(
            targetValue = targetAmp,
            animationSpec = tween(durationMillis = 80, easing = LinearEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .testTag("agent_audio_visualizer_wave"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val heightPx = size.height
            val centerY = heightPx / 2f

            if (width <= 0 || heightPx <= 0) return@Canvas

            val amp = smoothedAmplitude.value
            val maxWaveHeight = heightPx * 0.42f * amp

            // Color palette modulation based on agent state
            val baseColor = when (state) {
                AgentState.Thinking -> StatusThinking
                AgentState.Speaking -> primaryColor
                AgentState.Listening -> primaryColor
                AgentState.Connecting -> secondaryColor
                AgentState.Disconnected -> Color(0xFF475569)
                AgentState.Connected -> accentColor
            }

            val strokeWidthPx = lineWidth.toPx()

            // 1. Draw soft ambient back glow path
            val glowColor = baseColor.copy(alpha = 0.22f)
            drawHarmonicWave(
                centerY = centerY,
                width = width,
                maxAmplitude = maxWaveHeight * 1.08f,
                phase = phase,
                frequency = 1.0f,
                harmonicRatio = 0.25f,
                stroke = Stroke(
                    width = strokeWidthPx * 4.2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                color = glowColor
            )

            // 2. Draw secondary harmonic wave (indigo/accent shift with phase lead)
            val shiftedColor = if (state == AgentState.Thinking) {
                primaryColor.copy(alpha = 0.6f)
            } else {
                secondaryColor.copy(alpha = 0.55f + colorShift * 0.3f)
            }
            drawHarmonicWave(
                centerY = centerY,
                width = width,
                maxAmplitude = maxWaveHeight * 0.72f,
                phase = phase * 1.35f + 1.2f,
                frequency = 1.6f,
                harmonicRatio = 0.35f,
                stroke = Stroke(
                    width = strokeWidthPx * 0.85f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                color = shiftedColor
            )

            // 3. Draw tertiary detail harmonic wave (teal shimmer)
            if (state == AgentState.Speaking || state == AgentState.Listening) {
                drawHarmonicWave(
                    centerY = centerY,
                    width = width,
                    maxAmplitude = maxWaveHeight * 0.48f,
                    phase = -phase * 0.9f + 2.4f,
                    frequency = 2.2f,
                    harmonicRatio = 0.2f,
                    stroke = Stroke(
                        width = strokeWidthPx * 0.65f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    ),
                    color = accentColor.copy(alpha = 0.45f)
                )
            }

            // 4. Draw primary high-definition wave (LiveKit Cyan #1FD5F9)
            val primaryBrush = Brush.horizontalGradient(
                colors = listOf(
                    baseColor.copy(alpha = 0.3f),
                    baseColor,
                    if (state == AgentState.Speaking) secondaryColor else baseColor,
                    baseColor,
                    baseColor.copy(alpha = 0.3f)
                )
            )

            drawHarmonicWave(
                centerY = centerY,
                width = width,
                maxAmplitude = maxWaveHeight,
                phase = phase,
                frequency = 1.0f,
                harmonicRatio = 0.3f,
                stroke = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                brush = primaryBrush
            )
        }
    }
}

/**
 * Computes and renders a multi-harmonic sinusoidal wave with edge-tapering window.
 */
private fun DrawScope.drawHarmonicWave(
    centerY: Float,
    width: Float,
    maxAmplitude: Float,
    phase: Float,
    frequency: Float,
    harmonicRatio: Float,
    stroke: Stroke,
    color: Color? = null,
    brush: Brush? = null
) {
    val path = Path()
    val steps = 140
    val dx = width / steps

    for (i in 0..steps) {
        val x = i * dx
        // Normalized position along the width [0, 1]
        val progress = (x / width).coerceIn(0f, 1f)

        // Hann window envelope: drops cleanly to 0 at edges, peaks at center
        val envelope = sin(PI * progress).toFloat().let { it * it }

        // Multi-harmonic sine combination (fundamental + 2nd overtone)
        val theta = progress * 2f * PI.toFloat() * frequency + phase
        val harmonicTheta = progress * 4f * PI.toFloat() * frequency + (phase * 1.5f)

        val waveSample = sin(theta) + harmonicRatio * sin(harmonicTheta)
        val y = centerY + (waveSample * maxAmplitude * envelope)

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    if (brush != null) {
        drawPath(path = path, brush = brush, style = stroke)
    } else if (color != null) {
        drawPath(path = path, color = color, style = stroke)
    }
}
