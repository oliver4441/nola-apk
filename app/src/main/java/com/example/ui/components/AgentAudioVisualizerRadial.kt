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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AgentState
import com.example.ui.theme.LiveKitCyan
import com.example.ui.theme.LiveKitIndigo
import com.example.ui.theme.LiveKitTeal
import com.example.ui.theme.StatusThinking
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * LiveKit Agents UI Radial Visualizer Component.
 * Circular frequency equalizer bars expanding from a central hub.
 */
@Composable
fun AgentAudioVisualizerRadial(
    state: AgentState,
    audioAmplitude: Float,
    modifier: Modifier = Modifier,
    height: Dp = 260.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadialTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AgentState.Thinking -> 2800
                    AgentState.Speaking -> 4500
                    else -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadialRotation"
    )

    val smoothedAmplitude = remember { Animatable(0.08f) }
    LaunchedEffect(audioAmplitude, state) {
        val target = when (state) {
            AgentState.Disconnected -> 0.02f
            AgentState.Connecting -> 0.15f
            AgentState.Connected -> 0.1f
            AgentState.Listening -> (audioAmplitude * 0.9f + 0.08f).coerceIn(0.08f, 1f)
            AgentState.Thinking -> 0.45f
            AgentState.Speaking -> (audioAmplitude * 1.05f + 0.35f).coerceIn(0.35f, 1f)
        }
        smoothedAmplitude.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 70, easing = LinearEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .testTag("agent_audio_visualizer_radial"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val innerRadius = (size.minDimension / 2f) * 0.32f
            val maxBarLength = (size.minDimension / 2f) * 0.42f
            val amp = smoothedAmplitude.value

            val primaryColor = when (state) {
                AgentState.Thinking -> StatusThinking
                AgentState.Speaking -> LiveKitCyan
                AgentState.Listening -> LiveKitCyan
                AgentState.Connecting -> LiveKitIndigo
                AgentState.Disconnected -> Color(0xFF475569)
                AgentState.Connected -> LiveKitTeal
            }

            val secondaryColor = when (state) {
                AgentState.Thinking -> Color(0xFFEC4899)
                AgentState.Speaking -> LiveKitIndigo
                AgentState.Listening -> LiveKitTeal
                AgentState.Connecting -> LiveKitCyan
                AgentState.Disconnected -> Color(0xFF334155)
                AgentState.Connected -> LiveKitIndigo
            }

            // Draw center hub glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.5f),
                        primaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = innerRadius * 1.4f
                ),
                radius = innerRadius * 1.4f,
                center = center
            )

            // Draw center ring
            drawCircle(
                color = primaryColor.copy(alpha = 0.8f),
                radius = innerRadius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // Draw radial spectrum pins
            val barCount = 44
            val angleStep = (2 * PI / barCount).toFloat()
            val baseAngleRad = (rotation * PI / 180f).toFloat()

            for (i in 0 until barCount) {
                val angle = i * angleStep + baseAngleRad
                // Synthesize frequency height based on bar index harmonic and amplitude
                val harmonic = (sin(i * 0.8 + rotation * 0.05).toFloat() * 0.5f + 0.5f)
                val barLen = (maxBarLength * amp * harmonic).coerceAtLeast(4.dp.toPx())

                val startX = center.x + innerRadius * cos(angle)
                val startY = center.y + innerRadius * sin(angle)
                val endX = center.x + (innerRadius + barLen) * cos(angle)
                val endY = center.y + (innerRadius + barLen) * sin(angle)

                val barColor = if (i % 2 == 0) primaryColor else secondaryColor

                drawLine(
                    color = barColor.copy(alpha = (0.4f + amp * 0.6f).coerceIn(0.4f, 1f)),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
