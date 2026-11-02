package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AgentState
import com.example.ui.icons.PhosphorIcons
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LiveKitCyan
import com.example.ui.theme.StatusDanger

/**
 * LiveKit Agents UI Control Bar.
 * Provides quick access to microphone mute/unmute, end call/disconnect,
 * live transcripts toggle, audio output toggle, and server settings.
 */
@Composable
fun AgentControlBar(
    isMicMuted: Boolean,
    isSpeakerOn: Boolean,
    isTranscriptVisible: Boolean,
    agentState: AgentState,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleTranscript: () -> Unit,
    onEndSession: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("agent_control_bar"),
        color = DarkSurface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(32.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Settings button
            ControlButton(
                icon = PhosphorIcons.Gear,
                contentDescription = "Server Settings",
                testTag = "settings_button",
                onClick = onOpenSettings
            )

            // 2. Transcript Drawer toggle
            ControlButton(
                icon = PhosphorIcons.ChatTeardropText,
                contentDescription = "Toggle Transcripts",
                testTag = "transcript_toggle_button",
                isActive = isTranscriptVisible,
                onClick = onToggleTranscript
            )

            // 3. Central Primary Microphone Button (Gemini Live / Agents UI hero button)
            HeroMicButton(
                isMuted = isMicMuted,
                agentState = agentState,
                onClick = onToggleMic
            )

            // 4. Audio output switch (Speaker / Earpiece)
            ControlButton(
                icon = PhosphorIcons.SpeakerHigh,
                contentDescription = if (isSpeakerOn) "Speakerphone On" else "Speakerphone Off",
                testTag = "speaker_toggle_button",
                isActive = isSpeakerOn,
                onClick = onToggleSpeaker
            )

            // 5. End Session / Disconnect
            ControlButton(
                icon = PhosphorIcons.PhoneDisconnect,
                contentDescription = "End Call",
                testTag = "end_call_button",
                tint = StatusDanger,
                backgroundColor = StatusDanger.copy(alpha = 0.15f),
                onClick = onEndSession
            )
        }
    }
}

@Composable
private fun HeroMicButton(
    isMuted: Boolean,
    agentState: AgentState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MicPulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MicPulseScale"
    )

    val isActiveListening = !isMuted && agentState == AgentState.Listening
    val buttonBgColor by animateColorAsState(
        targetValue = when {
            isMuted -> DarkSurfaceVariant
            isActiveListening -> LiveKitCyan
            else -> LiveKitCyan.copy(alpha = 0.85f)
        },
        label = "MicBgColor"
    )

    val iconColor = if (isMuted) Color(0xFF94A3B8) else Color(0xFF0B0F17)

    Box(
        modifier = modifier
            .size(68.dp)
            .testTag("hero_mic_button"),
        contentAlignment = Alignment.Center
    ) {
        // Glowing pulse halo when actively listening
        if (isActiveListening) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(LiveKitCyan.copy(alpha = 0.25f))
            )
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(buttonBgColor)
                .border(
                    width = if (isMuted) 1.dp else 0.dp,
                    color = if (isMuted) DarkBorder else Color.Transparent,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 28.dp),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isMuted) PhosphorIcons.MicrophoneSlash else PhosphorIcons.Microphone,
                contentDescription = if (isMuted) "Unmute Microphone" else "Mute Microphone",
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    tint: Color = Color.White,
    backgroundColor: Color = if (isActive) LiveKitCyan.copy(alpha = 0.18f) else DarkSurfaceVariant.copy(alpha = 0.6f)
) {
    val activeTint = if (isActive) LiveKitCyan else tint

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 24.dp),
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = activeTint,
            modifier = Modifier.size(22.dp)
        )
    }
}
