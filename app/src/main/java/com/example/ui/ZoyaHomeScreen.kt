package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AssistantState
import com.example.ui.components.OrbVisualizer
import com.example.ui.components.SassyPromptChips
import com.example.ui.components.TranscriptDrawer
import com.example.ui.components.WaveformVisualizer
import com.example.ui.components.ZoyaHeaderCard
import com.example.ui.components.ZoyaSettingsSheet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ZoyaHomeScreen(
    viewModel: ZoyaViewModel
) {
    val context = LocalContext.current

    val state by viewModel.state.collectAsState()
    val emotion by viewModel.currentEmotion.collectAsState()
    val lastSubtitle by viewModel.lastSubtitle.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()

    var showTranscript by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    // Audio Permission Request Handling
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.toggleMicListening()
        }
    }

    val onMicClicked = {
        if (hasAudioPermission) {
            viewModel.toggleMicListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = ObsidianDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ObsidianDark,
                            Color(0xFF130A24),
                            ObsidianDark
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = NeonMagenta.copy(alpha = 0.2f),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ZOYA LIVE ENGINE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { showTranscript = true },
                            modifier = Modifier.testTag("open_transcript_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "Open Transcript Logs",
                                tint = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.testTag("open_settings_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Open Settings",
                                tint = TextPrimary
                            )
                        }
                    }
                }

                // Header Avatar & Subtitle Quote Card
                ZoyaHeaderCard(
                    emotion = emotion,
                    state = state,
                    lastSubtitle = lastSubtitle,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Central Orb & Visualizer section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    OrbVisualizer(
                        state = state,
                        amplitude = amplitude,
                        onClick = onMicClicked
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Audio Waveform
                    WaveformVisualizer(
                        state = state,
                        amplitude = amplitude,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                }

                // Sassy Quick Prompt Chips & Input Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SassyPromptChips(
                        onPromptSelected = { prompt ->
                            viewModel.handleUserSpeechInput(prompt)
                        },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Text Input Bar for hybrid voice/text fallback
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = {
                                Text(
                                    text = "Say or type something witty...",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (textInput.isNotBlank()) {
                                        viewModel.handleUserSpeechInput(textInput)
                                        textInput = ""
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ObsidianCard,
                                unfocusedContainerColor = ObsidianCard,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = ObsidianCard,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("speech_text_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    viewModel.handleUserSpeechInput(textInput)
                                    textInput = ""
                                } else {
                                    onMicClicked()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(NeonMagenta, NeonViolet)
                                    ),
                                    shape = CircleShape
                                )
                                .testTag("send_input_button")
                        ) {
                            Icon(
                                imageVector = if (textInput.isNotBlank()) Icons.Default.Send else Icons.Default.Mic,
                                contentDescription = "Send or Record",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Sheet Drawers
        if (showTranscript) {
            TranscriptDrawer(
                messages = messages,
                onDismiss = { showTranscript = false },
                onSpeakMessage = { text -> viewModel.speakMessageDirectly(text) },
                onShareMessage = { text -> viewModel.shareMessageDirectly(text) }
            )
        }

        if (showSettings) {
            ZoyaSettingsSheet(
                settings = settings,
                onSettingsChange = { newSettings -> viewModel.updateSettings(newSettings) },
                onDismiss = { showSettings = false }
            )
        }
    }
}
