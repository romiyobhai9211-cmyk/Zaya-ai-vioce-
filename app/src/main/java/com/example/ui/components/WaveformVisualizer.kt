package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .testTag("waveform_canvas")
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val isActive = state == AssistantState.LISTENING || state == AssistantState.SPEAKING
        val baseAmp = if (isActive) (amplitude.coerceIn(0.15f, 1f) * (height / 2.5f)) else (height / 12f)

        // Draw sine wave 1 (Magenta)
        val path1 = Path()
        path1.moveTo(0f, centerY)
        val steps = 80
        val stepX = width / steps

        for (i in 0..steps) {
            val x = i * stepX
            val normalizedX = x / width
            // Window function so ends taper down nicely
            val window = sin(normalizedX * Math.PI.toFloat())
            val y = centerY + sin(normalizedX * 4 * Math.PI.toFloat() + phaseShift) * baseAmp * window
            path1.lineTo(x, y.toFloat())
        }

        drawPath(
            path = path1,
            color = NeonMagenta.copy(alpha = if (isActive) 0.85f else 0.4f),
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw sine wave 2 (Cyan - inverted phase)
        val path2 = Path()
        path2.moveTo(0f, centerY)
        for (i in 0..steps) {
            val x = i * stepX
            val normalizedX = x / width
            val window = sin(normalizedX * Math.PI.toFloat())
            val y = centerY + sin(normalizedX * 4 * Math.PI.toFloat() - phaseShift + 1f) * (baseAmp * 0.75f) * window
            path2.lineTo(x, y.toFloat())
        }

        drawPath(
            path = path2,
            color = NeonCyan.copy(alpha = if (isActive) 0.8f else 0.35f),
            style = Stroke(width = 2.dp.toPx())
        )

        // Equalizer spectrum bars in background
        val barCount = 16
        val barGap = width / (barCount * 2)
        val barWidth = barGap * 0.8f

        for (b in 0 until barCount) {
            val barX = b * (barWidth + barGap) + barGap
            val barFactor = sin((b.toFloat() / barCount) * Math.PI.toFloat() + phaseShift).coerceIn(0.1f, 1f)
            val barHeight = if (isActive) (baseAmp * 1.8f * barFactor) else (height * 0.15f * barFactor)

            drawRect(
                color = if (b % 2 == 0) NeonViolet.copy(alpha = 0.35f) else NeonGreen.copy(alpha = 0.3f),
                topLeft = androidx.compose.ui.geometry.Offset(barX, centerY - barHeight / 2f),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )
        }
    }
}
