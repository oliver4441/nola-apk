package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AgentState
import com.example.model.VisualizerType
import com.example.ui.components.AgentAudioVisualizerAura
import com.example.ui.components.AgentAudioVisualizerBar
import com.example.ui.components.AgentAudioVisualizerRadial
import com.example.ui.components.AgentAudioVisualizerWave
import com.example.ui.components.AgentControlBar
import com.example.ui.components.AgentMemorySheet
import com.example.ui.components.AgentTranscriptDrawer
import com.example.ui.components.SettingsDialog
import com.example.ui.icons.PhosphorIcons
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LiveKitCyan
import com.example.ui.theme.LiveKitIndigo
import com.example.ui.theme.LiveKitTeal
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusListening
import com.example.ui.theme.StatusSpeaking
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusThinking
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AgentViewModel

@Composable
fun AgentVoiceScreen(
    viewModel: AgentViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val agentState by viewModel.agentState.collectAsState()
    val audioAmplitude by viewModel.audioAmplitude.collectAsState()
    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isTranscriptVisible by viewModel.isTranscriptVisible.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val config by viewModel.config.collectAsState()
    val transcripts by viewModel.transcripts.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val latencyMs by viewModel.latencyMs.collectAsState()
    val selectedVisualizer by viewModel.selectedVisualizer.collectAsState()
    val isMemorySheetOpen by viewModel.isMemorySheetOpen.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val sttProviderName by viewModel.sttProviderName.collectAsState()
    val isSpeechmaticsCreditDepleted by viewModel.isSpeechmaticsCreditDepleted.collectAsState()

    // Runtime Permission for Microphone Audio Record
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startMicCapture()
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            viewModel.startMicCapture()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("agent_voice_screen"),
        containerColor = DarkBackground,
        bottomBar = {
            AgentControlBar(
                isMicMuted = isMicMuted,
                isSpeakerOn = isSpeakerOn,
                isTranscriptVisible = isTranscriptVisible,
                agentState = agentState,
                onToggleMic = { viewModel.toggleMic() },
                onToggleSpeaker = { viewModel.toggleSpeaker() },
                onToggleTranscript = { viewModel.toggleTranscript() },
                onEndSession = {
                    if (agentState == AgentState.Disconnected) {
                        viewModel.reconnectSession()
                    } else {
                        viewModel.endSession()
                    }
                },
                onOpenSettings = { viewModel.openSettings() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBackground,
                            DarkSurface.copy(alpha = 0.5f),
                            DarkBackground
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header: Branding, State Badge, Mem0 & Latency
                TopHeaderBar(
                    agentState = agentState,
                    latencyMs = latencyMs,
                    memoriesCount = memories.size,
                    sttProviderName = sttProviderName,
                    isSpeechmaticsCreditDepleted = isSpeechmaticsCreditDepleted,
                    onOpenMemory = { viewModel.openMemorySheet() },
                    onOpenSettings = { viewModel.openSettings() }
                )

                Spacer(modifier = Modifier.weight(0.7f))

                // Agent State Indicator & Typography
                AgentStatusHeadline(
                    agentState = agentState,
                    statusMessage = statusMessage
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Visualizer Style Switcher Bar
                VisualizerStyleSelector(
                    selectedType = selectedVisualizer,
                    onSelectType = { viewModel.setVisualizer(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // The Active LiveKit Visualizer Template
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectedVisualizer) {
                        VisualizerType.Aura -> AgentAudioVisualizerAura(
                            state = agentState,
                            audioAmplitude = audioAmplitude,
                            modifier = Modifier.fillMaxSize(),
                            height = 240.dp
                        )
                        VisualizerType.Radial -> AgentAudioVisualizerRadial(
                            state = agentState,
                            audioAmplitude = audioAmplitude,
                            modifier = Modifier.fillMaxSize(),
                            height = 240.dp
                        )
                        VisualizerType.Bar -> AgentAudioVisualizerBar(
                            state = agentState,
                            audioAmplitude = audioAmplitude,
                            modifier = Modifier.fillMaxSize(),
                            height = 240.dp
                        )
                        VisualizerType.Wave -> AgentAudioVisualizerWave(
                            state = agentState,
                            audioAmplitude = audioAmplitude,
                            modifier = Modifier.fillMaxSize(),
                            height = 240.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Prompt Suggestions (Gemini Live style)
                if (agentState != AgentState.Disconnected) {
                    QuickPromptChips(
                        onSelectPrompt = { prompt ->
                            viewModel.simulateUserPrompt(prompt)
                        }
                    )
                } else {
                    Button(
                        onClick = { viewModel.reconnectSession() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LiveKitCyan,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("reconnect_button")
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.ArrowsClockwise,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reconnect to Agent",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            // Real-Time Transcript Drawer Overlay
            AgentTranscriptDrawer(
                isVisible = isTranscriptVisible,
                transcripts = transcripts,
                onClose = { viewModel.toggleTranscript() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Mem0 Memory Sheet Overlay
            AgentMemorySheet(
                isVisible = isMemorySheetOpen,
                memories = memories,
                onClose = { viewModel.closeMemorySheet() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Settings & Connection Dialog
            if (isSettingsOpen) {
                SettingsDialog(
                    config = config,
                    onSaveConfig = { newConfig -> viewModel.updateConfig(newConfig) },
                    onFetchToken = { endpoint -> viewModel.fetchToken(endpoint) },
                    onDismiss = { viewModel.closeSettings() }
                )
            }
        }
    }
}

@Composable
private fun TopHeaderBar(
    agentState: AgentState,
    latencyMs: Int,
    memoriesCount: Int,
    sttProviderName: String,
    isSpeechmaticsCreditDepleted: Boolean,
    onOpenMemory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App title & Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onOpenSettings)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, LiveKitCyan.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.nola_avatar_logo_1793604638432),
                        contentDescription = "Nola Avatar Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Nola",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Gemini + Mem0 Voice",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Mem0 Button & State Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mem0 Memory Chip
                Surface(
                    color = LiveKitIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LiveKitIndigo.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable(onClick = onOpenMemory)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Sparkle,
                            contentDescription = "Mem0 Memory",
                            tint = LiveKitIndigo,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mem0 ($memoriesCount)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }

                StateBadge(agentState = agentState)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // STT Provider Banner / Fallback indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isSpeechmaticsCreditDepleted) StatusWarning.copy(alpha = 0.12f) else LiveKitCyan.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSpeechmaticsCreditDepleted) StatusWarning.copy(alpha = 0.4f) else LiveKitCyan.copy(alpha = 0.25f)
                )
            ) {
                Text(
                    text = if (isSpeechmaticsCreditDepleted) "STT: Android On-Device (Speechmatics Credit Depleted)" else "STT: Speechmatics AI",
                    fontSize = 10.sp,
                    color = if (isSpeechmaticsCreditDepleted) StatusWarning else LiveKitCyan,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Text(
                text = "${latencyMs}ms ping",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun StateBadge(agentState: AgentState) {
    val infiniteTransition = rememberInfiniteTransition(label = "BadgePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BadgePulseScale"
    )

    val badgeColor = when (agentState) {
        AgentState.Listening -> StatusListening
        AgentState.Speaking -> StatusSpeaking
        AgentState.Thinking -> StatusThinking
        AgentState.Connecting -> StatusWarning
        AgentState.Connected -> StatusSuccess
        AgentState.Disconnected -> StatusDanger
    }

    Surface(
        color = badgeColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(if (agentState != AgentState.Disconnected) pulseScale else 1.0f)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = agentState.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = badgeColor
            )
        }
    }
}

@Composable
private fun AgentStatusHeadline(
    agentState: AgentState,
    statusMessage: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = when (agentState) {
                AgentState.Listening -> "Nola is Listening"
                AgentState.Speaking -> "Nola is Responding"
                AgentState.Thinking -> "Nola is Thinking..."
                AgentState.Connecting -> "Connecting to Nola"
                AgentState.Connected -> "Nola Active"
                AgentState.Disconnected -> "Nola Disconnected"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun QuickPromptChips(
    onSelectPrompt: (String) -> Unit
) {
    val prompts = listOf(
        "Run system diagnostic",
        "Summarize recent logs",
        "Check agent task queue"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Suggested Voice Queries",
            fontSize = 11.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            prompts.forEach { prompt ->
                Surface(
                    color = DarkSurfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.clickable { onSelectPrompt(prompt) }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VisualizerStyleSelector(
    selectedType: VisualizerType,
    onSelectType: (VisualizerType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurfaceVariant.copy(alpha = 0.85f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier.testTag("visualizer_style_selector")
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VisualizerType.entries.forEach { type ->
                val isSelected = type == selectedType
                val bgColor = if (isSelected) LiveKitCyan else Color.Transparent
                val textColor = if (isSelected) DarkBackground else TextSecondary

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .clickable { onSelectType(type) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.displayName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}
