package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * LiveKit Agents UI Aura Visualizer Component.
 * Port of the LiveKit @agents-ui/agent-audio-visualizer-aura component to native Jetpack Compose.
 *
 * Implements:
 * - Fluid glowing organic orb inspired by Gemini Live and LiveKit Aura
 * - Concentric multi-layer radial gradient halos with organic fluid edge deformation
 * - Dynamic color transitions: cyan (#1FD5F9), indigo (#818CF8), electric purple (#A855F7), and teal (#2DD4BF)
 * - Reactive speech amplitude pulses and rotational swirl during Thinking state
 */
@Composable
fun AgentAudioVisualizerAura(
    state: AgentState,
    audioAmplitude: Float,
    modifier: Modifier = Modifier,
    height: Dp = 260.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraInfiniteTransition")

    // Continuous rotation angle for thinking and breathing states
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AgentState.Thinking -> 3000
                    AgentState.Speaking -> 5000
                    else -> 9000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuraRotation"
    )

    // Breathing pulse for idle & listening states
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraBreathing"
    )

    // Smooth amplitude transition
    val smoothedAmplitude = remember { Animatable(0.08f) }
    LaunchedEffect(audioAmplitude, state) {
        val target = when (state) {
            AgentState.Disconnected -> 0.02f
            AgentState.Connecting -> 0.15f
            AgentState.Connected -> 0.12f
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
            .testTag("agent_audio_visualizer_aura"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.42f
            val amp = smoothedAmplitude.value
            val pulse = if (state == AgentState.Listening || state == AgentState.Connected) breathingPulse else 1f
            val activeRadius = baseRadius * pulse * (1f + amp * 0.45f)

            if (activeRadius <= 0f) return@Canvas

            // Dynamic color selection based on state
            val primary = when (state) {
                AgentState.Thinking -> StatusThinking
                AgentState.Speaking -> LiveKitCyan
                AgentState.Listening -> LiveKitCyan
                AgentState.Connecting -> LiveKitIndigo
                AgentState.Disconnected -> Color(0xFF475569)
                AgentState.Connected -> LiveKitTeal
            }

            val secondary = when (state) {
                AgentState.Thinking -> Color(0xFFEC4899) // Electric Pink/Magenta
                AgentState.Speaking -> LiveKitIndigo
                AgentState.Listening -> LiveKitTeal
                AgentState.Connecting -> LiveKitCyan
                AgentState.Disconnected -> Color(0xFF334155)
                AgentState.Connected -> LiveKitIndigo
            }

            // 1. Outermost Ambient Atmospheric Glow
            val outerGlowRadius = activeRadius * 2.2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.28f * amp.coerceAtLeast(0.2f)),
                        secondary.copy(alpha = 0.12f * amp.coerceAtLeast(0.15f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerGlowRadius
                ),
                radius = outerGlowRadius,
                center = center
            )

            // 2. Secondary Harmonic Ripple Halo
            val secondaryHaloRadius = activeRadius * 1.45f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondary.copy(alpha = 0.35f),
                        primary.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = secondaryHaloRadius
                ),
                radius = secondaryHaloRadius,
                center = center
            )

            // 3. Fluid Organic Lobe Path (Deformed circle reacting to frequencies)
            val fluidPath = Path()
            val points = 60
            val angleStep = (2 * PI / points).toFloat()
            val radAngle = (rotationAngle * PI / 180f).toFloat()

            for (i in 0 until points) {
                val theta = i * angleStep
                // Multi-harmonic radial displacement
                val harmonic1 = sin((theta * 3f + radAngle).toDouble()).toFloat() * (amp * 16.dp.toPx())
                val harmonic2 = cos((theta * 5f - radAngle * 1.5f).toDouble()).toFloat() * (amp * 10.dp.toPx())
                val r = activeRadius + harmonic1 + harmonic2
                val x = center.x + r * cos(theta)
                val y = center.y + r * sin(theta)

                if (i == 0) {
                    fluidPath.moveTo(x, y)
                } else {
                    fluidPath.lineTo(x, y)
                }
            }
            fluidPath.close()

            // Draw fluid organic mesh body
            drawPath(
                path = fluidPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        primary.copy(alpha = 0.75f),
                        secondary.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = activeRadius * 1.15f
                )
            )

            // 4. Fluid glowing edge ring
            drawPath(
                path = fluidPath,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primary,
                        secondary,
                        LiveKitTeal,
                        primary
                    ),
                    center = center
                ),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // 5. Bright Core Luminescence
            val innerCoreRadius = activeRadius * 0.38f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primary.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = innerCoreRadius
                ),
                radius = innerCoreRadius,
                center = center
            )
        }
    }
}
