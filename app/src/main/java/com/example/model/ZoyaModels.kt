package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.NeonGreen

enum class AssistantState {
    IDLE,
    CONNECTING,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

enum class ZoyaEmotion(val label: String, val emoji: String, val accentColor: Color) {
    CONFIDENT("Confident", "💅", NeonMagenta),
    SASSY("Sassy", "😏", NeonCyan),
    FLIRTY("Flirty", "😉", NeonViolet),
    PLAYFUL("Playful", "😜", NeonGreen),
    TEASING("Teasing", "🔥", NeonMagenta),
    THINKING("Thinking", "🤔", NeonCyan)
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val emotion: ZoyaEmotion = ZoyaEmotion.CONFIDENT,
    val toolCallName: String? = null,
    val toolCallArg: String? = null
)

enum class MessageSender {
    USER,
    ZOYA,
    SYSTEM
}

data class ZoyaSettings(
    val speechPitch: Float = 1.25f, // Slightly higher female pitch for sassy tone
    val speechRate: Float = 1.05f,  // Quick, confident rate
    val sassLevel: Int = 4,         // 1 to 5 scale
    val functionCallingEnabled: Boolean = true,
    val continuousVoiceMode: Boolean = true,
    val activeVoiceName: String = "Female Default"
)

data class ToolExecutionLog(
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String,
    val argument: String,
    val success: Boolean,
    val resultMessage: String
)
