package com.example.data

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.ZoyaSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

class AudioEngine(
    private val context: Context,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private val TAG = "AudioEngine"

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    // State flows
    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    // Callbacks
    var onSpeechResult: ((String) -> Unit)? = null
    var onSpeechStart: (() -> Unit)? = null
    var onSpeechEnd: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    init {
        initializeTts()
        initializeRecognizer()
    }

    private fun initializeTts() {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            textToSpeech?.run {
                language = Locale.US
                setPitch(1.25f) // Confident, sassy female voice pitch
                setSpeechRate(1.05f)

                // Try to find a nice female voice
                voices?.find { voice ->
                    voice.name.contains("female", ignoreCase = true) ||
                            voice.name.contains("en-us-x-sfg", ignoreCase = true) ||
                            voice.name.contains("en_us", ignoreCase = true)
                }?.let { selectedVoice ->
                    voice = selectedVoice
                }

                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _amplitude.value = 0f
                        onSpeechEnd?.invoke()
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _amplitude.value = 0f
                        _isSpeaking.value = false
                    }
                })
            }
        } else {
            Log.e(TAG, "TTS Init failed with status: $status")
        }
    }

    private fun initializeRecognizer() {
        scope.launch(Dispatchers.Main) {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                            _recognizedText.value = ""
                            playChime(440f, 150)
                            onSpeechStart?.invoke()
                        }

                        override fun onBeginningOfSpeech() {
                            _isListening.value = true
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Map rmsdB (-2 to 10 approx) to 0.0 - 1.0 amplitude
                            val norm = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _amplitude.value = norm
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _isListening.value = false
                        }

                        override fun onError(error: Int) {
                            _isListening.value = false
                            _amplitude.value = 0f
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                else -> "Recognition error code: $error"
                            }
                            Log.d(TAG, "Speech Error: $msg")
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            _amplitude.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotBlank()) {
                                _recognizedText.value = text
                                onSpeechResult?.invoke(text)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotBlank()) {
                                _recognizedText.value = text
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } else {
                Log.w(TAG, "Speech recognition is not available on this device.")
            }
        }
    }

    fun startListening() {
        scope.launch(Dispatchers.Main) {
            stopSpeaking()
            if (speechRecognizer == null) {
                initializeRecognizer()
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognizer", e)
                onError?.invoke("Mic error: ${e.localizedMessage}")
            }
        }
    }

    fun stopListening() {
        scope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
                _isListening.value = false
                _amplitude.value = 0f
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping listener", e)
            }
        }
    }

    fun speak(text: String, settings: ZoyaSettings = ZoyaSettings()) {
        if (!isTtsInitialized || textToSpeech == null) {
            Log.w(TAG, "TTS not ready yet")
            return
        }

        stopListening()
        textToSpeech?.run {
            setPitch(settings.speechPitch)
            setSpeechRate(settings.speechRate)

            // Simulate waveform amplitude while speaking
            simulateSpeakingAmplitude(text.length)

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "zoya_talk_${System.currentTimeMillis()}")
            }
            speak(text, TextToSpeech.QUEUE_FLUSH, params, "zoya_talk_${System.currentTimeMillis()}")
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
        _amplitude.value = 0f
    }

    private fun simulateSpeakingAmplitude(textLength: Int) {
        scope.launch(Dispatchers.Default) {
            val durationMs = (textLength * 65).coerceIn(1000, 10000)
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < durationMs && _isSpeaking.value) {
                val cycle = ((System.currentTimeMillis() - startTime) % 400) / 400f
                val wave = (sin(cycle * Math.PI * 2) * 0.4f + 0.5f).toFloat()
                _amplitude.value = wave.coerceIn(0.2f, 0.95f)
                kotlinx.coroutines.delay(50)
            }
            _amplitude.value = 0f
        }
    }

    fun playChime(freqHz: Float = 587.33f, durationMs: Int = 200) {
        scope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * durationMs / 1000)
                val samples = ShortArray(numSamples)
                val alpha = 2.0 * Math.PI * freqHz / sampleRate
                for (i in 0 until numSamples) {
                    val envelope = 1f - (i.toFloat() / numSamples)
                    val sample = (sin(i * alpha) * 16383 * envelope).toInt().toShort()
                    samples[i] = sample
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
            } catch (e: Exception) {
                Log.d(TAG, "AudioTrack chime playback skipped: ${e.message}")
            }
        }
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
