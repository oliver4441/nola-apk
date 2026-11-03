package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.AgentState
import com.example.model.VisualizerType
import com.example.ui.components.AgentAudioVisualizerAura
import com.example.ui.components.AgentAvatarVisualizer
import com.example.ui.components.AgentAudioVisualizerBar
import com.example.ui.components.AgentAudioVisualizerRadial
import com.example.ui.components.AgentAudioVisualizerWave
import com.example.ui.components.AgentChatView
import com.example.ui.components.AgentControlBar
import com.example.ui.components.AgentMemorySheet
import com.example.ui.components.AgentSessionHistorySheet
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
    val isChatMode by viewModel.isChatMode.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isHistoryOpen by viewModel.isHistoryOpen.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val config by viewModel.config.collectAsState()
    val transcripts by viewModel.transcripts.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val latencyMs by viewModel.latencyMs.collectAsState()
    val selectedVisualizer by viewModel.selectedVisualizer.collectAsState()
    val isMemorySheetOpen by viewModel.isMemorySheetOpen.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val sttProviderName by viewModel.sttProviderName.collectAsState()
    val isSpeechmaticsCreditDepleted by viewModel.isSpeechmaticsCreditDepleted.collectAsState()

    // Back handler: return from Chat mode to Voice visualizer mode
    BackHandler(enabled = isChatMode) {
        viewModel.setChatMode(false)
    }

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
            if (!isChatMode) {
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
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header: Branding, Mode Switcher, History, Mem0 & Settings
                TopHeaderBar(
                    agentState = agentState,
                    isChatMode = isChatMode,
                    sessionsCount = sessions.size,
                    memoriesCount = memories.size,
                    sttProviderName = sttProviderName,
                    isSpeechmaticsCreditDepleted = isSpeechmaticsCreditDepleted,
                    onToggleMode = { viewModel.toggleChatMode() },
                    onOpenHistory = { viewModel.openHistory() },
                    onOpenMemory = { viewModel.openMemorySheet() },
                    onOpenSettings = { viewModel.openSettings() }
                )

                if (isChatMode) {
                    // Full Interactive Chat View
                    AgentChatView(
                        transcripts = transcripts,
                        agentState = agentState,
                        statusMessage = statusMessage,
                        onSendMessage = { text -> viewModel.sendTextMessage(text) },
                        onPlaySpeech = { text -> viewModel.speakTextAloud(text) },
                        onToggleMic = { viewModel.toggleMic() },
                        isMicActive = !isMicMuted,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    // Voice Visualizer Mode
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

                    // Active LiveKit Visualizer Template
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (selectedVisualizer) {
                            VisualizerType.Avatar -> AgentAvatarVisualizer(
                                state = agentState,
                                audioAmplitude = audioAmplitude,
                                modifier = Modifier.fillMaxSize()
                            )
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
                                viewModel.processUserVoiceInput(prompt)
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
            }

            // Real-Time Transcript Drawer Overlay (in Voice mode)
            if (!isChatMode) {
                AgentTranscriptDrawer(
                    isVisible = isTranscriptVisible,
                    transcripts = transcripts,
                    onClose = { viewModel.toggleTranscript() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Session History Sheet
            if (isHistoryOpen) {
                AgentSessionHistorySheet(
                    sessions = sessions,
                    currentSessionId = currentSessionId,
                    onSelectSession = { id -> viewModel.loadSession(id) },
                    onNewSession = { viewModel.startNewSession() },
                    onDeleteSession = { id -> viewModel.deleteSession(id) },
                    onClearAll = { viewModel.clearAllSessions() },
                    onDismiss = { viewModel.closeHistory() }
                )
            }

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
    isChatMode: Boolean,
    sessionsCount: Int,
    memoriesCount: Int,
    sttProviderName: String,
    isSpeechmaticsCreditDepleted: Boolean,
    onToggleMode: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App title & Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically
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
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Nola",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isChatMode) "Chat Mode" else "Voice Mode",
                        fontSize = 11.sp,
                        color = LiveKitCyan
                    )
                }
            }

            // Header Actions: Mode toggle, History, Mem0, Settings
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Voice/Chat Toggle Pill
                Surface(
                    color = if (isChatMode) LiveKitCyan.copy(alpha = 0.15f) else DarkSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isChatMode) LiveKitCyan else DarkBorder
                    ),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable(onClick = onToggleMode)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isChatMode) PhosphorIcons.Waveform else PhosphorIcons.ChatTeardropText,
                            contentDescription = if (isChatMode) "Switch to Voice" else "Switch to Chat",
                            tint = if (isChatMode) LiveKitCyan else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isChatMode) "Voice" else "Chat",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isChatMode) LiveKitCyan else TextPrimary
                        )
                    }
                }

                // History Button
                IconButton(
                    onClick = onOpenHistory,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.ClockCounterClockwise,
                        contentDescription = "Session History",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Mem0 Memory Button
                Surface(
                    color = LiveKitIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LiveKitIndigo.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clickable(onClick = onOpenMemory)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Sparkle,
                            contentDescription = "Mem0",
                            tint = LiveKitIndigo,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$memoriesCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Settings Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Gear,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // STT Status Banner
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
                    text = if (isSpeechmaticsCreditDepleted) "On-device fallback" else sttProviderName,
                    fontSize = 10.sp,
                    color = if (isSpeechmaticsCreditDepleted) StatusWarning else LiveKitCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }

            StateBadge(agentState = agentState)
        }
    }
}

@Composable
private fun StateBadge(agentState: AgentState) {
    val badgeColor by animateColorAsState(
        targetValue = when (agentState) {
            AgentState.Disconnected -> StatusDanger
            AgentState.Connecting -> StatusWarning
            AgentState.Connected -> StatusSuccess
            AgentState.Listening -> StatusListening
            AgentState.Thinking -> StatusThinking
            AgentState.Speaking -> StatusSpeaking
        },
        label = "StateBadgeColor"
    )

    Surface(
        color = badgeColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = agentState.label,
                fontSize = 10.sp,
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
                AgentState.Disconnected -> "Agent Inactive"
                AgentState.Connecting -> "Connecting..."
                AgentState.Connected -> "Nola is Online"
                AgentState.Listening -> "Listening to You"
                AgentState.Thinking -> "Nola is Processing"
                AgentState.Speaking -> "Nola is Speaking"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun VisualizerStyleSelector(
    selectedType: VisualizerType,
    onSelectType: (VisualizerType) -> Unit
) {
    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(3.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            VisualizerType.entries.forEach { type ->
                val isSelected = selectedType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) LiveKitCyan.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) LiveKitCyan else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectType(type) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.displayName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) LiveKitCyan else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPromptChips(
    onSelectPrompt: (String) -> Unit
) {
    val prompts = listOf(
        "Summarize conversation",
        "What tasks are pending?",
        "What is the system status?"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Suggested Questions",
            fontSize = 11.sp,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            prompts.forEach { prompt ->
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectPrompt(prompt) }
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
