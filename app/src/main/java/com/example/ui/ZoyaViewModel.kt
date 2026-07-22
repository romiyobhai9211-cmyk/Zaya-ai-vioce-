package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AudioEngine
import com.example.data.GeminiLiveRepository
import com.example.data.GeminiResult
import com.example.data.ToolExecutor
import com.example.data.ZoyaPersona
import com.example.model.AssistantState
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.model.ZoyaEmotion
import com.example.model.ZoyaSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ZoyaViewModel(application: Application) : AndroidViewModel(application) {

    private val audioEngine = AudioEngine(application.applicationContext, viewModelScope)
    private val repository = GeminiLiveRepository()
    private val toolExecutor = ToolExecutor(application.applicationContext)

    // State Flows
    private val _state = MutableStateFlow(AssistantState.IDLE)
    val state: StateFlow<AssistantState> = _state.asStateFlow()

    private val _currentEmotion = MutableStateFlow(ZoyaEmotion.CONFIDENT)
    val currentEmotion: StateFlow<ZoyaEmotion> = _currentEmotion.asStateFlow()

    private val _lastSubtitle = MutableStateFlow(ZoyaPersona.getRandomSassyIntro())
    val lastSubtitle: StateFlow<String> = _lastSubtitle.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _settings = MutableStateFlow(ZoyaSettings())
    val settings: StateFlow<ZoyaSettings> = _settings.asStateFlow()

    val amplitude: StateFlow<Float> = audioEngine.amplitude
    val isListening: StateFlow<Boolean> = audioEngine.isListening
    val isSpeaking: StateFlow<Boolean> = audioEngine.isSpeaking

    init {
        // Welcome message
        val introMsg = ZoyaPersona.getRandomSassyIntro()
        addMessage(MessageSender.ZOYA, introMsg, ZoyaEmotion.CONFIDENT)

        // Wire AudioEngine Callbacks
        audioEngine.onSpeechResult = { text ->
            handleUserSpeechInput(text)
        }

        audioEngine.onSpeechStart = {
            _state.value = AssistantState.LISTENING
        }

        audioEngine.onSpeechEnd = {
            if (_state.value == AssistantState.LISTENING) {
                _state.value = AssistantState.THINKING
            }
        }

        audioEngine.onError = { error ->
            _state.value = AssistantState.ERROR
            _lastSubtitle.value = "Mic glitch: $error. Tap to retry darling!"
        }
    }

    fun toggleMicListening() {
        if (audioEngine.isListening.value) {
            audioEngine.stopListening()
            _state.value = AssistantState.IDLE
        } else {
            audioEngine.stopSpeaking()
            audioEngine.startListening()
            _state.value = AssistantState.LISTENING
            _lastSubtitle.value = "Listening to you... try to impress me! 😏"
        }
    }

    fun handleUserSpeechInput(promptText: String) {
        if (promptText.isBlank()) return

        // 1. Record User Message
        addMessage(MessageSender.USER, promptText)
        _state.value = AssistantState.THINKING
        _lastSubtitle.value = "Processing your vibe..."

        // 2. Perform Gemini Query in background
        viewModelScope.launch {
            val historyPairs = _messages.value.map {
                (if (it.sender == MessageSender.USER) "User" else "Zoya") to it.text
            }

            when (val result = repository.generateZoyaResponse(promptText, historyPairs)) {
                is GeminiResult.Success -> {
                    _currentEmotion.value = result.emotion
                    _lastSubtitle.value = result.text

                    // Check if function call tool was triggered
                    var toolName: String? = null
                    var toolArg: String? = null

                    if (result.functionCall != null && _settings.value.functionCallingEnabled) {
                        toolName = result.functionCall.name
                        val args = result.functionCall.arguments
                        toolArg = args.values.joinToString(", ")

                        when (toolName) {
                            "openWebsite" -> {
                                val url = args["url"] ?: "https://google.com"
                                val label = args["label"]
                                toolExecutor.openWebsite(url, label)
                            }
                            "checkDeviceStatus" -> {
                                toolExecutor.checkDeviceStatus()
                            }
                            "setReminder" -> {
                                val title = args["title"] ?: "Zoya Reminder"
                                val mins = args["minutes"]?.toIntOrNull() ?: 5
                                toolExecutor.setQuickReminder(title, mins)
                            }
                            "expressEmotion" -> {
                                val emoStr = args["emotion"] ?: "SASSY"
                                try {
                                    _currentEmotion.value = ZoyaEmotion.valueOf(emoStr)
                                } catch (e: Exception) {
                                    _currentEmotion.value = ZoyaEmotion.SASSY
                                }
                            }
                        }
                    }

                    // Add Zoya response
                    addMessage(
                        sender = MessageSender.ZOYA,
                        text = result.text,
                        emotion = result.emotion,
                        toolCallName = toolName,
                        toolCallArg = toolArg
                    )

                    // Speak response aloud with TTS
                    _state.value = AssistantState.SPEAKING
                    audioEngine.speak(result.text, _settings.value)
                }

                is GeminiResult.Error -> {
                    _state.value = AssistantState.ERROR
                    _lastSubtitle.value = result.message
                }
            }
        }
    }

    fun speakMessageDirectly(text: String) {
        _state.value = AssistantState.SPEAKING
        audioEngine.speak(text, _settings.value)
    }

    fun shareMessageDirectly(text: String) {
        toolExecutor.shareText(text)
    }

    fun updateSettings(newSettings: ZoyaSettings) {
        _settings.value = newSettings
    }

    private fun addMessage(
        sender: MessageSender,
        text: String,
        emotion: ZoyaEmotion = ZoyaEmotion.CONFIDENT,
        toolCallName: String? = null,
        toolCallArg: String? = null
    ) {
        val newMsg = ChatMessage(
            sender = sender,
            text = text,
            emotion = emotion,
            toolCallName = toolCallName,
            toolCallArg = toolCallArg
        )
        _messages.value = _messages.value + newMsg
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
