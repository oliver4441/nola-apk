package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.model.AgentState
import com.example.ui.theme.LiveKitCyan
import com.example.ui.theme.LiveKitIndigo
import com.example.ui.theme.LiveKitTeal
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * A lightweight, native, dimensional character portrait. The mouth is driven by the same
 * normalized speech envelope used by the existing voice visualizer/TTS callbacks.
 */
@Composable
fun AgentAvatarVisualizer(
    state: AgentState,
    audioAmplitude: Float,
    modifier: Modifier = Modifier
) {
    val mouthOpen = remember { Animatable(0.04f) }
    val blink = remember { Animatable(1f) }
    var useSpeakingFallback by remember { mutableStateOf(false) }
    val voiceTransition = rememberInfiniteTransition(label = "AvatarVoiceFallback")
    val fallbackJaw by voiceTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FallbackJaw"
    )

    // TTS publishes a changing amplitude envelope. A remote LiveKit speaker event currently
    // exposes speaking state only, so use a gentle jaw animation if that envelope stays still.
    LaunchedEffect(state, audioAmplitude) {
        useSpeakingFallback = false
        if (state == AgentState.Speaking) {
            delay(350L)
            useSpeakingFallback = true
        }
    }

    LaunchedEffect(state, audioAmplitude) {
        val target = if (state == AgentState.Speaking) {
            ((audioAmplitude - 0.08f) / 0.82f).coerceIn(0.08f, 1f)
        } else {
            0.035f
        }
        mouthOpen.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = if (state == AgentState.Speaking) 58 else 170,
                easing = FastOutSlowInEasing
            )
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2200L, 4800L))
            blink.animateTo(0.08f, tween(durationMillis = 75))
            delay(65L)
            blink.animateTo(1f, tween(durationMillis = 115))
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("nola_avatar_visualizer")
            .semantics { contentDescription = "Nola animated voice avatar" }
    ) {
        val unit = size.minDimension
        if (unit <= 0f) return@Canvas

        val cx = size.width / 2f
        val faceWidth = unit * 0.58f
        val faceHeight = unit * 0.70f
        val faceLeft = cx - faceWidth / 2f
        val faceTop = size.height * 0.08f
        val faceBottom = faceTop + faceHeight
        val faceSize = Size(faceWidth, faceHeight)
        val speaking = state == AgentState.Speaking
        val accent = when (state) {
            AgentState.Thinking -> LiveKitIndigo
            AgentState.Speaking -> LiveKitCyan
            AgentState.Listening -> LiveKitTeal
            AgentState.Connecting -> LiveKitIndigo
            AgentState.Disconnected -> Color(0xFF475569)
            AgentState.Connected -> LiveKitTeal
        }

        // Ambient halo gives the portrait the soft, dimensional presence of the old orb.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = if (speaking) 0.24f else 0.13f), Color.Transparent),
                center = Offset(cx, faceTop + faceHeight * 0.48f),
                radius = unit * 0.62f
            ),
            radius = unit * 0.62f,
            center = Offset(cx, faceTop + faceHeight * 0.48f)
        )
        drawCircle(
            color = accent.copy(alpha = 0.16f),
            radius = unit * 0.49f,
            center = Offset(cx, faceTop + faceHeight * 0.48f),
            style = Stroke(width = 1.dp.toPx())
        )

        // Shoulders and clothing, drawn behind the neck and face.
        val shoulderPath = Path().apply {
            moveTo(cx - unit * 0.49f, size.height)
            lineTo(cx - unit * 0.44f, size.height * 0.88f)
            cubicTo(cx - unit * 0.37f, size.height * 0.80f, cx - unit * 0.25f, size.height * 0.78f, cx, size.height * 0.78f)
            cubicTo(cx + unit * 0.25f, size.height * 0.78f, cx + unit * 0.37f, size.height * 0.80f, cx + unit * 0.44f, size.height * 0.88f)
            lineTo(cx + unit * 0.49f, size.height)
            close()
        }
        drawPath(
            path = shoulderPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF344C68), Color(0xFF172337), Color(0xFF101827)),
                start = Offset(cx - unit * 0.45f, size.height * 0.78f),
                end = Offset(cx + unit * 0.4f, size.height)
            )
        )
        drawPath(
            path = shoulderPath,
            color = LiveKitCyan.copy(alpha = 0.24f),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Neck with a soft side shadow.
        val neckWidth = faceWidth * 0.30f
        val neckTop = faceTop + faceHeight * 0.70f
        val neckHeight = size.height * 0.20f
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF9A5F50), Color(0xFFE5B39A), Color(0xFFB87967)),
                startX = cx - neckWidth / 2f,
                endX = cx + neckWidth / 2f
            ),
            topLeft = Offset(cx - neckWidth / 2f, neckTop),
            size = Size(neckWidth, neckHeight)
        )

        // Hair silhouette behind the face, with warm highlights for depth.
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF55362D), Color(0xFF211B21), Color(0xFF111722)),
                center = Offset(cx - faceWidth * 0.18f, faceTop + faceHeight * 0.28f),
                radius = faceWidth * 0.95f
            ),
            topLeft = Offset(faceLeft - faceWidth * 0.10f, faceTop - faceHeight * 0.07f),
            size = Size(faceWidth * 1.20f, faceHeight * 1.17f)
        )

        // Ears peek out on either side of the face.
        val earY = faceTop + faceHeight * 0.48f
        drawOval(
            color = Color(0xFFB97F6B),
            topLeft = Offset(faceLeft - unit * 0.045f, earY - unit * 0.065f),
            size = Size(unit * 0.10f, unit * 0.15f)
        )
        drawOval(
            color = Color(0xFFDDA58D),
            topLeft = Offset(faceLeft + faceWidth - unit * 0.055f, earY - unit * 0.065f),
            size = Size(unit * 0.10f, unit * 0.15f)
        )

        // Face: warm skin gradient and a subtle light source from the upper-left.
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFD5B8), Color(0xFFEAB39A), Color(0xFFC78671)),
                center = Offset(faceLeft + faceWidth * 0.34f, faceTop + faceHeight * 0.28f),
                radius = faceWidth * 1.02f
            ),
            topLeft = Offset(faceLeft, faceTop),
            size = faceSize
        )

        // Side locks frame the cheeks, while leaving the face clear.
        val leftLock = Path().apply {
            moveTo(faceLeft + faceWidth * 0.12f, faceTop + faceHeight * 0.12f)
            cubicTo(faceLeft - faceWidth * 0.08f, faceTop + faceHeight * 0.35f, faceLeft + faceWidth * 0.02f, faceTop + faceHeight * 0.72f, faceLeft + faceWidth * 0.18f, faceTop + faceHeight * 0.85f)
            cubicTo(faceLeft + faceWidth * 0.09f, faceTop + faceHeight * 0.60f, faceLeft + faceWidth * 0.20f, faceTop + faceHeight * 0.35f, faceLeft + faceWidth * 0.32f, faceTop + faceHeight * 0.20f)
            close()
        }
        drawPath(leftLock, Brush.linearGradient(listOf(Color(0xFF5A382F), Color(0xFF211A20)), start = Offset(faceLeft, faceTop), end = Offset(faceLeft + faceWidth * 0.2f, faceBottom)))
        val rightLock = Path().apply {
            moveTo(faceLeft + faceWidth * 0.88f, faceTop + faceHeight * 0.12f)
            cubicTo(faceLeft + faceWidth * 1.08f, faceTop + faceHeight * 0.35f, faceLeft + faceWidth * 0.98f, faceTop + faceHeight * 0.72f, faceLeft + faceWidth * 0.82f, faceTop + faceHeight * 0.85f)
            cubicTo(faceLeft + faceWidth * 0.91f, faceTop + faceHeight * 0.60f, faceLeft + faceWidth * 0.80f, faceTop + faceHeight * 0.35f, faceLeft + faceWidth * 0.68f, faceTop + faceHeight * 0.20f)
            close()
        }
        drawPath(rightLock, Brush.linearGradient(listOf(Color(0xFF211A20), Color(0xFF5A382F)), start = Offset(faceLeft + faceWidth * 0.78f, faceTop), end = Offset(faceLeft + faceWidth, faceBottom)))

        // Brows and expressive eyes. Blink scale compresses the eye opening naturally.
        val eyeY = faceTop + faceHeight * 0.43f
        val eyeGap = faceWidth * 0.20f
        val eyeWidth = faceWidth * 0.17f
        val eyeHeight = unit * 0.025f * blink.value.coerceAtLeast(0.08f)
        for (side in listOf(-1f, 1f)) {
            val eyeCx = cx + side * eyeGap
            val brow = Path().apply {
                moveTo(eyeCx - eyeWidth * 0.62f, eyeY - unit * 0.075f)
                quadraticTo(eyeCx, eyeY - unit * 0.12f, eyeCx + eyeWidth * 0.62f, eyeY - unit * 0.065f)
            }
            drawPath(brow, Color(0xFF67443B), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            if (blink.value > 0.16f) {
                drawOval(
                    color = Color(0xFFFFF5EA),
                    topLeft = Offset(eyeCx - eyeWidth / 2f, eyeY - eyeHeight / 2f),
                    size = Size(eyeWidth, eyeHeight)
                )
                val irisRadius = eyeHeight * 0.46f
                drawCircle(Color(0xFF4B302C), irisRadius, Offset(eyeCx, eyeY))
                drawCircle(Color(0xFFFFFFFF), irisRadius * 0.24f, Offset(eyeCx - irisRadius * 0.28f, eyeY - irisRadius * 0.30f))
            } else {
                drawLine(
                    color = Color(0xFF68453D),
                    start = Offset(eyeCx - eyeWidth * 0.48f, eyeY),
                    end = Offset(eyeCx + eyeWidth * 0.48f, eyeY),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Nose bridge and tip highlight.
        val nose = Path().apply {
            moveTo(cx + unit * 0.008f, eyeY + unit * 0.025f)
            quadraticTo(cx - unit * 0.018f, eyeY + unit * 0.115f, cx + unit * 0.015f, eyeY + unit * 0.13f)
        }
        drawPath(nose, Color(0xFFB77B69).copy(alpha = 0.68f), style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round))
        drawLine(
            color = Color(0xFFFFE0C9).copy(alpha = 0.70f),
            start = Offset(cx - unit * 0.012f, eyeY + unit * 0.125f),
            end = Offset(cx + unit * 0.035f, eyeY + unit * 0.128f),
            strokeWidth = 1.3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Soft cheek color adds a little warmth without obscuring the portrait.
        for (side in listOf(-1f, 1f)) {
            drawCircle(
                color = Color(0xFFE88F88).copy(alpha = 0.18f),
                radius = unit * 0.045f,
                center = Offset(cx + side * faceWidth * 0.29f, faceTop + faceHeight * 0.60f)
            )
        }

        // Mouth opening follows the normalized speech envelope: closed smile at rest,
        // naturally opening and closing while Nola's voice amplitude changes.
        val mouthY = faceTop + faceHeight * 0.72f
        val openness = if (speaking && useSpeakingFallback) fallbackJaw else mouthOpen.value
        val mouthAmount = openness.coerceIn(0f, 1f)
        val mouthWidth = faceWidth * (0.18f + mouthAmount * 0.06f)
        val mouthHeight = unit * (0.008f + mouthAmount * 0.070f)
        if (speaking && mouthAmount > 0.08f) {
            drawOval(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4A2028), Color(0xFF7D3039), Color(0xFF321923)),
                    startY = mouthY - mouthHeight / 2f,
                    endY = mouthY + mouthHeight / 2f
                ),
                topLeft = Offset(cx - mouthWidth / 2f, mouthY - mouthHeight / 2f),
                size = Size(mouthWidth, mouthHeight)
            )
            if (mouthAmount > 0.42f) {
                drawOval(
                    color = Color(0xFFFFD5C9),
                    topLeft = Offset(cx - mouthWidth * 0.30f, mouthY - mouthHeight * 0.30f),
                    size = Size(mouthWidth * 0.60f, mouthHeight * 0.20f)
                )
            }
            drawArc(
                color = Color(0xFF9C4E56),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - mouthWidth * 0.57f, mouthY - mouthHeight * 0.53f),
                size = Size(mouthWidth * 1.14f, mouthHeight * 1.06f),
                style = Stroke(width = 1.4.dp.toPx())
            )
        } else {
            val smile = Path().apply {
                moveTo(cx - mouthWidth * 0.56f, mouthY)
                quadraticTo(cx, mouthY + unit * 0.035f, cx + mouthWidth * 0.56f, mouthY)
            }
            drawPath(smile, Color(0xFF9B4F53), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            drawLine(
                color = Color(0xFFFFD0B8).copy(alpha = 0.72f),
                start = Offset(cx - mouthWidth * 0.44f, mouthY + unit * 0.008f),
                end = Offset(cx + mouthWidth * 0.44f, mouthY + unit * 0.008f),
                strokeWidth = 0.7.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Swept fringe and a pair of hair highlights finish the portrait silhouette.
        val fringe = Path().apply {
            moveTo(faceLeft + faceWidth * 0.07f, faceTop + faceHeight * 0.20f)
            cubicTo(faceLeft + faceWidth * 0.10f, faceTop - faceHeight * 0.02f, faceLeft + faceWidth * 0.40f, faceTop - faceHeight * 0.07f, faceLeft + faceWidth * 0.57f, faceTop + faceHeight * 0.11f)
            cubicTo(faceLeft + faceWidth * 0.72f, faceTop + faceHeight * 0.02f, faceLeft + faceWidth * 0.93f, faceTop + faceHeight * 0.10f, faceLeft + faceWidth * 0.93f, faceTop + faceHeight * 0.27f)
            cubicTo(faceLeft + faceWidth * 0.78f, faceTop + faceHeight * 0.21f, faceLeft + faceWidth * 0.71f, faceTop + faceHeight * 0.20f, faceLeft + faceWidth * 0.61f, faceTop + faceHeight * 0.31f)
            cubicTo(faceLeft + faceWidth * 0.47f, faceTop + faceHeight * 0.19f, faceLeft + faceWidth * 0.31f, faceTop + faceHeight * 0.25f, faceLeft + faceWidth * 0.24f, faceTop + faceHeight * 0.33f)
            cubicTo(faceLeft + faceWidth * 0.19f, faceTop + faceHeight * 0.26f, faceLeft + faceWidth * 0.13f, faceTop + faceHeight * 0.24f, faceLeft + faceWidth * 0.07f, faceTop + faceHeight * 0.20f)
            close()
        }
        drawPath(fringe, Brush.linearGradient(listOf(Color(0xFF684236), Color(0xFF2B2022), Color(0xFF171820)), start = Offset(faceLeft, faceTop), end = Offset(cx + faceWidth * 0.4f, faceTop + faceHeight * 0.3f)))
        val hairShine = Path().apply {
            moveTo(faceLeft + faceWidth * 0.19f, faceTop + faceHeight * 0.13f)
            cubicTo(faceLeft + faceWidth * 0.35f, faceTop + faceHeight * 0.02f, faceLeft + faceWidth * 0.53f, faceTop + faceHeight * 0.03f, faceLeft + faceWidth * 0.63f, faceTop + faceHeight * 0.11f)
        }
        drawPath(hairShine, Color(0xFFD49A76).copy(alpha = 0.40f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))

        // A subtle cyan edge light makes the character belong in Nola's voice UI.
        drawCircle(
            color = accent.copy(alpha = if (state == AgentState.Disconnected) 0.10f else 0.30f),
            radius = unit * 0.47f,
            center = Offset(cx, faceTop + faceHeight * 0.48f),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
