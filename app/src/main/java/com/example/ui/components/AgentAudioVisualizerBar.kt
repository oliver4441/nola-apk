package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * LiveKit Agents UI Bar Visualizer Component.
 * Vertical audio spectrum equalizer bars.
 */
@Composable
fun AgentAudioVisualizerBar(
    state: AgentState,
    audioAmplitude: Float,
    modifier: Modifier = Modifier,
    height: Dp = 260.dp
) {
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
            animationSpec = tween(durationMillis = 60, easing = LinearEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .testTag("agent_audio_visualizer_bar"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val centerY = size.height / 2f
            val maxBarHeight = size.height * 0.42f
            val amp = smoothedAmplitude.value

            val barCount = 28
            val barSpacing = 4.dp.toPx()
            val totalSpacing = (barCount - 1) * barSpacing
            val availableWidth = (totalWidth * 0.85f)
            val barWidth = ((availableWidth - totalSpacing) / barCount).coerceIn(4.dp.toPx(), 10.dp.toPx())
            val startX = (totalWidth - (barCount * barWidth + totalSpacing)) / 2f

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

            for (i in 0 until barCount) {
                val progress = i.toFloat() / (barCount - 1).toFloat()
                // Bell curve shape for natural audio spectrum
                val bellCurve = sin(progress * PI).toFloat()
                val barHeight = (maxBarHeight * amp * (0.3f + 0.7f * bellCurve)).coerceAtLeast(6.dp.toPx())

                val x = startX + i * (barWidth + barSpacing)
                val y = centerY - (barHeight / 2f)

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor,
                            secondaryColor
                        ),
                        startY = y,
                        endY = y + barHeight
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
