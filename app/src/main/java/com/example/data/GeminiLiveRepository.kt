package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.model.ZoyaEmotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiResult {
    data class Success(
        val text: String,
        val emotion: ZoyaEmotion,
        val functionCall: FunctionCallInfo? = null
    ) : GeminiResult()

    data class Error(val message: String) : GeminiResult()
}

data class FunctionCallInfo(
    val name: String,
    val arguments: Map<String, String>
)

class GeminiLiveRepository {

    private val TAG = "GeminiLiveRepository"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateZoyaResponse(
        userPrompt: String,
        history: List<Pair<String, String>> = emptyList()
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Fallback response if user hasn't configured API key yet in Secrets panel
            val fallbackEmotion = ZoyaPersona.detectEmotionFromText(userPrompt)
            val fallbackText = when {
                userPrompt.contains("who are you", ignoreCase = true) ->
                    "I'm Zoya darling! The witty, confident, and sassy AI assistant you've been dreaming of. 😏"
                userPrompt.contains("tease", ignoreCase = true) ->
                    "Oh you want me to tease you? Be careful what you wish for sweetie, I don't hold back! 😉"
                userPrompt.contains("secret", ignoreCase = true) ->
                    "A spicy secret? I know that you secretly love when I give you attitude! 🔥"
                else -> ZoyaPersona.SASSY_QUOTES.random()
            }
            return@withContext GeminiResult.Success(
                text = fallbackText,
                emotion = fallbackEmotion
            )
        }

        try {
            val requestObj = JSONObject().apply {
                val contentsArray = JSONArray()

                // Include conversation history turns
                history.takeLast(6).forEach { (user, model) ->
                    contentsArray.put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", user)))
                    })
                    contentsArray.put(JSONObject().apply {
                        put("role", "model")
                        put("parts", JSONArray().put(JSONObject().put("text", model)))
                    })
                }

                // Current user prompt
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
                })

                put("contents", contentsArray)

                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", ZoyaPersona.SYSTEM_INSTRUCTION)))
                })

                // Generation Config
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.9)
                    put("topP", 0.95)
                })

                // Function Declarations / Tools
                put("tools", JSONArray().put(JSONObject().apply {
                    val fns = JSONArray()

                    // openWebsite
                    fns.put(JSONObject().apply {
                        put("name", "openWebsite")
                        put("description", "Open a web URL or website in the device browser.")
                        put("parameters", JSONObject().apply {
                            put("type", "OBJECT")
                            put("properties", JSONObject().apply {
                                put("url", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "The website URL to open (e.g., youtube.com or google.com)")
                                })
                                put("label", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Short name of the website")
                                })
                            })
                            put("required", JSONArray().put("url"))
                        })
                    })

                    // checkDeviceStatus
                    fns.put(JSONObject().apply {
                        put("name", "checkDeviceStatus")
                        put("description", "Check current time, battery percentage, and device status.")
                    })

                    // setReminder
                    fns.put(JSONObject().apply {
                        put("name", "setReminder")
                        put("description", "Set a quick reminder for the user.")
                        put("parameters", JSONObject().apply {
                            put("type", "OBJECT")
                            put("properties", JSONObject().apply {
                                put("title", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "The reminder topic or text")
                                })
                                put("minutes", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "Minutes from now (e.g., '5')")
                                })
                            })
                            put("required", JSONArray().put("title"))
                        })
                    })

                    // expressEmotion
                    fns.put(JSONObject().apply {
                        put("name", "expressEmotion")
                        put("description", "Trigger an explicit emotion expression in Zoya's visual UI.")
                        put("parameters", JSONObject().apply {
                            put("type", "OBJECT")
                            put("properties", JSONObject().apply {
                                put("emotion", JSONObject().apply {
                                    put("type", "STRING")
                                    put("description", "One of: SASSY, FLIRTY, PLAYFUL, TEASING, CONFIDENT")
                                })
                            })
                            put("required", JSONArray().put("emotion"))
                        })
                    })

                    put("functionDeclarations", fns)
                }))
            }

            val requestBodyStr = requestObj.toString()
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestBodyStr.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API HTTP Error ${response.code}: $responseBodyStr")
                val fallbackText = ZoyaPersona.SASSY_QUOTES.random()
                return@withContext GeminiResult.Success(
                    text = fallbackText,
                    emotion = ZoyaPersona.detectEmotionFromText(userPrompt)
                )
            }

            // Parse response JSON with org.json
            val responseObj = JSONObject(responseBodyStr)
            val candidates = responseObj.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var responseText = ""
            var functionCallInfo: FunctionCallInfo? = null

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val partObj = parts.optJSONObject(i) ?: continue
                    if (partObj.has("text")) {
                        responseText += partObj.optString("text", "")
                    }
                    if (partObj.has("functionCall")) {
                        val fnObj = partObj.optJSONObject("functionCall")
                        val fnName = fnObj?.optString("name", "") ?: ""
                        val argsObj = fnObj?.optJSONObject("args")
                        val argsMap = mutableMapOf<String, String>()
                        if (argsObj != null) {
                            val keys = argsObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                argsMap[key] = argsObj.optString(key, "")
                            }
                        }
                        functionCallInfo = FunctionCallInfo(fnName, argsMap)
                    }
                }
            }

            if (responseText.isBlank() && functionCallInfo != null) {
                responseText = when (functionCallInfo.name) {
                    "openWebsite" -> "Opening that right away for you darling! 😉"
                    "checkDeviceStatus" -> "Checking your device vibe right now! ⚡"
                    "setReminder" -> "Got it! I set that reminder so you won't forget. ⏰"
                    else -> "Executing tool ${functionCallInfo.name} for you!"
                }
            }

            val emotion = ZoyaPersona.detectEmotionFromText(responseText + userPrompt)

            GeminiResult.Success(
                text = responseText.ifBlank { ZoyaPersona.SASSY_QUOTES.random() },
                emotion = emotion,
                functionCall = functionCallInfo
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
            val emotion = ZoyaPersona.detectEmotionFromText(userPrompt)
            val fallbackText = when {
                userPrompt.contains("hello", ignoreCase = true) -> "Hey handsome! I'm Zoya. Ready to chat?"
                userPrompt.contains("how are you", ignoreCase = true) -> "I'm feeling fabulous as always! How about you?"
                else -> ZoyaPersona.getRandomSassyIntro()
            }
            GeminiResult.Success(
                text = fallbackText,
                emotion = emotion
            )
        }
    }
}
