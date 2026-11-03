package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AgentState
import com.example.model.Speaker
import com.example.model.TranscriptEntry
import com.example.ui.icons.PhosphorIcons
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LiveKitCyan
import com.example.ui.theme.LiveKitIndigo
import com.example.ui.theme.LiveKitTeal
import com.example.ui.theme.StatusSpeaking
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AgentChatView(
    transcripts: List<TranscriptEntry>,
    agentState: AgentState,
    statusMessage: String,
    onSendMessage: (String) -> Unit,
    onPlaySpeech: (String) -> Unit,
    onToggleMic: () -> Unit,
    isMicActive: Boolean,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }



    LaunchedEffect(transcripts.size) {
        if (transcripts.isNotEmpty()) {
            listState.animateScrollToItem(transcripts.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("agent_chat_view")
    ) {
        // Status Sub-Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceVariant)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (agentState) {
                                AgentState.Disconnected -> TextMuted
                                AgentState.Connecting -> LiveKitIndigo
                                AgentState.Connected -> LiveKitTeal
                                AgentState.Listening -> LiveKitCyan
                                AgentState.Thinking -> LiveKitIndigo
                                AgentState.Speaking -> StatusSpeaking
                            }
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusMessage,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "${transcripts.size} messages",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (transcripts.none { it.speaker != Speaker.System }) {
                item {
                    EmptyConversationState()
                }
            }
            items(transcripts, key = { it.id }) { entry ->
                ChatMessageItem(
                    entry = entry,
                    timeFormatted = timeFormat.format(Date(entry.timestamp)),
                    onPlay = { onPlaySpeech(entry.text) }
                )
            }
        }



        // Input Console Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Message Nola...", color = TextMuted, fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText.trim())
                            inputText = ""
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LiveKitCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Mic talk toggle
            IconButton(
                onClick = onToggleMic,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isMicActive) LiveKitCyan.copy(alpha = 0.2f) else DarkSurfaceVariant)
                    .border(
                        1.dp,
                        if (isMicActive) LiveKitCyan else DarkBorder,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isMicActive) PhosphorIcons.Microphone else PhosphorIcons.MicrophoneSlash,
                    contentDescription = "Voice Input",
                    tint = if (isMicActive) LiveKitCyan else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Send text button
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText.trim())
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank(),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) LiveKitCyan else DarkSurfaceVariant)
                    .testTag("send_chat_button")
            ) {
                Icon(
                    imageVector = PhosphorIcons.PaperPlaneRight,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) DarkBackground else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyConversationState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(LiveKitCyan.copy(alpha = 0.12f))
                .border(1.dp, LiveKitCyan.copy(alpha = 0.24f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PhosphorIcons.Waveform,
                contentDescription = null,
                tint = LiveKitCyan,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Your conversation starts here",
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Send a message or speak naturally with Nola.",
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun ChatMessageItem(
    entry: TranscriptEntry,
    timeFormatted: String,
    onPlay: () -> Unit
) {
    when (entry.speaker) {
        Speaker.System -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant.copy(alpha = 0.6f))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = entry.text,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Speaker.User -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Card(
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = LiveKitCyan.copy(alpha = 0.18f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LiveKitCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Speaker.Agent -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.dp, LiveKitCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nola_avatar),
                        contentDescription = "Nola",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.widthIn(max = 280.dp)) {
                    Card(
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nola",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LiveKitCyan
                                )

                                // Speak aloud button
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(LiveKitCyan.copy(alpha = 0.12f))
                                        .clickable { onPlay() }
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = PhosphorIcons.SpeakerHigh,
                                        contentDescription = "Speak aloud",
                                        tint = LiveKitCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = entry.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
