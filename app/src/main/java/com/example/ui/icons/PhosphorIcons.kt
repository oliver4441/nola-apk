package com.example.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Authentic Phosphor Icons (https://phosphoricons.com) implemented as Jetpack Compose ImageVectors.
 * Designed on a 256x256 coordinate grid with 16dp stroke weights, rounded caps, and joins.
 */
object PhosphorIcons {

    val Microphone: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.Microphone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Mic capsule
                moveTo(128f, 24f)
                curveTo(105.9f, 24f, 88f, 41.9f, 88f, 64f)
                lineTo(88f, 128f)
                curveTo(88f, 150.1f, 105.9f, 168f, 128f, 168f)
                curveTo(150.1f, 168f, 168f, 150.1f, 168f, 128f)
                lineTo(168f, 64f)
                curveTo(168f, 41.9f, 150.1f, 24f, 128f, 24f)
                close()

                // Cradle arc
                moveTo(208f, 120f)
                lineTo(208f, 128f)
                curveTo(208f, 172.2f, 172.2f, 208f, 128f, 208f)
                curveTo(83.8f, 208f, 48f, 172.2f, 48f, 128f)
                lineTo(48f, 120f)

                // Stand
                moveTo(128f, 208f)
                lineTo(128f, 240f)
                moveTo(96f, 240f)
                lineTo(160f, 240f)
            }
        }.build()
    }

    val MicrophoneSlash: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.MicrophoneSlash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Slash
                moveTo(48f, 40f)
                lineTo(208f, 216f)

                // Partial capsule
                moveTo(128f, 24f)
                curveTo(105.9f, 24f, 88f, 41.9f, 88f, 64f)
                lineTo(88f, 84f)

                moveTo(168f, 108f)
                lineTo(168f, 64f)
                curveTo(168f, 41.9f, 150.1f, 24f, 128f, 24f)

                moveTo(104f, 162f)
                curveTo(111.4f, 165.9f, 119.5f, 168f, 128f, 168f)
                curveTo(150.1f, 168f, 168f, 150.1f, 168f, 128f)
                lineTo(168f, 120f)

                // Cradle arc
                moveTo(208f, 120f)
                curveTo(208f, 164.2f, 172.2f, 200f, 128f, 208f)
                moveTo(48f, 120f)
                curveTo(48f, 137.4f, 53.6f, 153.6f, 63.1f, 166.8f)

                // Stand
                moveTo(128f, 208f)
                lineTo(128f, 240f)
                moveTo(96f, 240f)
                lineTo(160f, 240f)
            }
        }.build()
    }

    val Waveform: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.Waveform",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Multi-frequency wave bars
                moveTo(24f, 128f)
                lineTo(24f, 128.1f)

                moveTo(59f, 96f)
                lineTo(59f, 160f)

                moveTo(93f, 64f)
                lineTo(93f, 192f)

                moveTo(128f, 32f)
                lineTo(128f, 224f)

                moveTo(163f, 64f)
                lineTo(163f, 192f)

                moveTo(197f, 96f)
                lineTo(197f, 160f)

                moveTo(232f, 128f)
                lineTo(232f, 128.1f)
            }
        }.build()
    }

    val ChatTeardropText: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.ChatTeardropText",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Speech bubble shape
                moveTo(132f, 216f)
                horizontalLineTo(47.7f)
                arcTo(7.7f, 7.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, 40f, 208.3f)
                verticalLineTo(124f)
                curveTo(40f, 75.4f, 79.4f, 36f, 128f, 36f)
                reflectiveCurveTo(216f, 75.4f, 216f, 124f)
                reflectiveCurveTo(176.6f, 216f, 132f, 216f)
                close()

                // Text lines inside bubble
                moveTo(96f, 112f)
                horizontalLineTo(160f)
                moveTo(96f, 144f)
                horizontalLineTo(136f)
            }
        }.build()
    }

    val SlidersHorizontal: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.SlidersHorizontal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Track 1
                moveTo(40f, 80f)
                lineTo(80f, 80f)
                moveTo(128f, 80f)
                lineTo(216f, 80f)
                // Knob 1
                moveTo(80f, 60f)
                lineTo(80f, 100f)
                lineTo(128f, 100f)
                lineTo(128f, 60f)
                close()

                // Track 2
                moveTo(40f, 176f)
                lineTo(144f, 176f)
                moveTo(192f, 176f)
                lineTo(216f, 176f)
                // Knob 2
                moveTo(144f, 156f)
                lineTo(144f, 196f)
                lineTo(192f, 196f)
                lineTo(192f, 156f)
                close()
            }
        }.build()
    }

    val Gear: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.Gear",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Center circle
                moveTo(128f, 100f)
                arcTo(28f, 28f, 0f, isMoreThanHalf = true, isPositiveArc = true, 128f, 156f)
                arcTo(28f, 28f, 0f, isMoreThanHalf = true, isPositiveArc = true, 128f, 100f)
                close()

                // Gear teeth contour
                moveTo(115.5f, 40f)
                lineTo(140.5f, 40f)
                lineTo(144.3f, 63.6f)
                lineTo(161f, 70.5f)
                lineTo(181.7f, 57.8f)
                lineTo(199.4f, 75.5f)
                lineTo(186.7f, 96.2f)
                lineTo(193.6f, 112.9f)
                lineTo(217.2f, 116.7f)
                lineTo(217.2f, 141.7f)
                lineTo(193.6f, 145.5f)
                lineTo(186.7f, 162.2f)
                lineTo(199.4f, 182.9f)
                lineTo(181.7f, 200.6f)
                lineTo(161f, 187.9f)
                lineTo(144.3f, 194.8f)
                lineTo(140.5f, 218.4f)
                lineTo(115.5f, 218.4f)
                lineTo(111.7f, 194.8f)
                lineTo(95f, 187.9f)
                lineTo(74.3f, 200.6f)
                lineTo(56.6f, 182.9f)
                lineTo(69.3f, 162.2f)
                lineTo(62.4f, 145.5f)
                lineTo(38.8f, 141.7f)
                lineTo(38.8f, 116.7f)
                lineTo(62.4f, 112.9f)
                lineTo(69.3f, 96.2f)
                lineTo(56.6f, 75.5f)
                lineTo(74.3f, 57.8f)
                lineTo(95f, 70.5f)
                lineTo(111.7f, 63.6f)
                close()
            }
        }.build()
    }

    val PhoneDisconnect: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.PhoneDisconnect",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Inverted receiver (hang up)
                moveTo(32f, 152f)
                curveTo(48f, 132f, 76f, 116f, 128f, 116f)
                curveTo(180f, 116f, 208f, 132f, 224f, 152f)
                lineTo(196f, 188f)
                curveTo(184f, 172f, 168f, 160f, 148f, 156f)
                lineTo(148f, 180f)
                horizontalLineTo(108f)
                lineTo(108f, 156f)
                curveTo(88f, 160f, 72f, 172f, 60f, 188f)
                close()
            }
        }.build()
    }

    val SpeakerHigh: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.SpeakerHigh",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Speaker cone
                moveTo(80f, 96f)
                horizontalLineTo(40f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 32f, 104f)
                verticalLineTo(152f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 40f, 160f)
                horizontalLineTo(80f)
                lineTo(152f, 208f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 160f, 200f)
                verticalLineTo(56f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = false, 152f, 48f)
                close()

                // Sound waves
                moveTo(196f, 100f)
                arcTo(40f, 40f, 0f, isMoreThanHalf = false, isPositiveArc = true, 196f, 156f)
                moveTo(224f, 76f)
                arcTo(76f, 76f, 0f, isMoreThanHalf = false, isPositiveArc = true, 224f, 180f)
            }
        }.build()
    }

    val ArrowsClockwise: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.ArrowsClockwise",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(176f, 40f)
                lineTo(216f, 40f)
                lineTo(216f, 80f)

                moveTo(216f, 40f)
                arcTo(96f, 96f, 0f, isMoreThanHalf = false, isPositiveArc = false, 56.6f, 64f)

                moveTo(80f, 216f)
                lineTo(40f, 216f)
                lineTo(40f, 176f)

                moveTo(40f, 216f)
                arcTo(96f, 96f, 0f, isMoreThanHalf = false, isPositiveArc = false, 199.4f, 192f)
            }
        }.build()
    }

    val Sparkle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.Sparkle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(128f, 24f)
                curveTo(128f, 80f, 176f, 128f, 232f, 128f)
                curveTo(176f, 128f, 128f, 176f, 128f, 232f)
                curveTo(128f, 176f, 80f, 128f, 24f, 128f)
                curveTo(80f, 128f, 128f, 80f, 128f, 24f)
                close()
            }
        }.build()
    }

    val X: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.X",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(200f, 56f)
                lineTo(56f, 200f)
                moveTo(200f, 200f)
                lineTo(56f, 56f)
            }
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(216f, 72f)
                lineTo(104f, 184f)
                lineTo(40f, 120f)
            }
        }.build()
    }

    val CaretDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.CaretDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(208f, 96f)
                lineTo(128f, 176f)
                lineTo(48f, 96f)
            }
        }.build()
    }

    val CaretUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.CaretUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(48f, 160f)
                lineTo(128f, 80f)
                lineTo(208f, 160f)
            }
        }.build()
    }

    val TerminalWindow: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.TerminalWindow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Window border
                moveTo(40f, 48f)
                horizontalLineTo(216f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 224f, 56f)
                verticalLineTo(200f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 216f, 208f)
                horizontalLineTo(40f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 32f, 200f)
                verticalLineTo(56f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 40f, 48f)
                close()

                // Header line
                moveTo(32f, 88f)
                lineTo(224f, 88f)

                // Prompt arrow
                moveTo(72f, 128f)
                lineTo(96f, 144f)
                lineTo(72f, 160f)

                // Cursor line
                moveTo(112f, 160f)
                horizontalLineTo(136f)
            }
        }.build()
    }

    val Key: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phosphor.Key",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 16f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Key bow (circle)
                moveTo(92f, 92f)
                arcTo(48f, 48f, 0f, isMoreThanHalf = true, isPositiveArc = true, 92f, 188f)
                arcTo(48f, 48f, 0f, isMoreThanHalf = true, isPositiveArc = true, 92f, 92f)
                close()

                // Key shaft
                moveTo(136f, 140f)
                lineTo(216f, 140f)
                lineTo(216f, 172f)

                // Key bit/teeth
                moveTo(184f, 140f)
                lineTo(184f, 164f)
            }
        }.build()
    }
}
