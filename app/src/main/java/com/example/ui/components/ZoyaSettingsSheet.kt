package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ZoyaSettings
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoyaSettingsSheet(
    settings: ZoyaSettings,
    onSettingsChange: (ZoyaSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianDark,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        modifier = modifier.testTag("settings_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Settings",
                        tint = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ZOYA VOICE & PERSONALITY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Settings",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security Warning Notice as mandated by Skills
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF22151B)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonMagenta.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Notice",
                        tint = NeonMagenta,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Security & API Configuration",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = NeonMagenta
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "I have configured Gemini API keys securely via BuildConfig (.env). Please enter your custom API key in the Secrets panel in AI Studio UI. Do not share generated APK files publicly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Voice Pitch Slider
            Text(
                text = "Voice Pitch: ${String.format("%.2f", settings.speechPitch)}x",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Higher pitch gives Zoya a bright, youthful female voice timbre.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Slider(
                value = settings.speechPitch,
                onValueChange = { onSettingsChange(settings.copy(speechPitch = it)) },
                valueRange = 0.8f..1.6f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonMagenta,
                    activeTrackColor = NeonMagenta,
                    inactiveTrackColor = ObsidianCard
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Speech Rate Slider
            Text(
                text = "Speech Rate: ${String.format("%.2f", settings.speechRate)}x",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Fast, confident talking tempo for quick retorts.",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Slider(
                value = settings.speechRate,
                onValueChange = { onSettingsChange(settings.copy(speechRate = it)) },
                valueRange = 0.7f..1.4f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = ObsidianCard
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sassiness Level
            Text(
                text = "Sassiness & Teasing Level: ${settings.sassLevel}/5 🔥",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Slider(
                value = settings.sassLevel.toFloat(),
                onValueChange = { onSettingsChange(settings.copy(sassLevel = it.toInt())) },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(
                    thumbColor = NeonMagenta,
                    activeTrackColor = NeonMagenta,
                    inactiveTrackColor = ObsidianCard
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Function Calling Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Browser & Device Function Calling",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Allows Zoya to open websites, check battery/time, and trigger Android actions.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = settings.functionCallingEnabled,
                    onCheckedChange = { onSettingsChange(settings.copy(functionCallingEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = ObsidianCard
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
