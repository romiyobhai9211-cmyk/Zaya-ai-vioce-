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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.GlowCyan
import com.example.ui.theme.GlowPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianCard

@Composable
fun OrbVisualizer(
    state: AssistantState,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")

    // Rotation angle for futuristic ring
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse scale for idle / listening / speaking
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Primary aura color based on state
    val mainGlowColor = when (state) {
        AssistantState.LISTENING -> NeonCyan
        AssistantState.SPEAKING -> NeonMagenta
        AssistantState.THINKING -> NeonViolet
        AssistantState.CONNECTING -> NeonViolet
        AssistantState.ERROR -> Color(0xFFFF3366)
        AssistantState.IDLE -> NeonMagenta
    }

    val secondaryGlowColor = when (state) {
        AssistantState.LISTENING -> NeonGreen
        AssistantState.SPEAKING -> NeonCyan
        else -> NeonViolet
    }

    val dynamicScale = remember(amplitude, state) {
        if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
            1f + (amplitude * 0.35f)
        } else if (state == AssistantState.THINKING) {
            pulseScale
        } else {
            1f
        }
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("orb_visualizer_container"),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing aura canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(dynamicScale)
        ) {
            val center = this.center
            val radius = size.minDimension / 2f

            // Outer multi-ring ripple
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(mainGlowColor.copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = radius * 1.1f
                ),
                radius = radius * 0.95f
            )

            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                // Expanding amplitude ring 1
                drawCircle(
                    color = mainGlowColor.copy(alpha = (0.6f * amplitude).coerceIn(0.1f, 0.8f)),
                    radius = radius * (0.7f + amplitude * 0.3f),
                    style = Stroke(width = 4.dp.toPx())
                )
                // Secondary expanding ring
                drawCircle(
                    color = secondaryGlowColor.copy(alpha = (0.4f * amplitude).coerceIn(0.05f, 0.5f)),
                    radius = radius * (0.8f + amplitude * 0.25f),
                    style = Stroke(width = 2.dp.toPx())
                )
            } else if (state == AssistantState.THINKING) {
                // Thinking dotted orbital arc
                drawCircle(
                    color = NeonViolet.copy(alpha = 0.7f),
                    radius = radius * 0.85f,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Center interactive Orb button
        Box(
            modifier = Modifier
                .size(130.dp)
                .scale(if (state == AssistantState.THINKING) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            mainGlowColor.copy(alpha = 0.85f),
                            ObsidianCard,
                            Color.Black
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(mainGlowColor, secondaryGlowColor, mainGlowColor)
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .testTag("central_mic_button"),
            contentAlignment = Alignment.Center
        ) {
            // Icon in the center
            val icon = when (state) {
                AssistantState.LISTENING -> Icons.Filled.Mic
                AssistantState.SPEAKING -> Icons.Filled.RecordVoiceOver
                AssistantState.THINKING -> Icons.Filled.Psychology
                AssistantState.CONNECTING -> Icons.Filled.Psychology
                AssistantState.ERROR -> Icons.Filled.MicOff
                AssistantState.IDLE -> Icons.Filled.Mic
            }

            Icon(
                imageVector = icon,
                contentDescription = "Zoya Voice Mic",
                modifier = Modifier
                    .size(48.dp)
                    .testTag("orb_icon"),
                tint = Color.White
            )
        }
    }
}
