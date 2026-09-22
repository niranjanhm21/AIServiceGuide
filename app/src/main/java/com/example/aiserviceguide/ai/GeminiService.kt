package com.example.aiserviceguide.ai

import com.example.aiserviceguide.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiService {

    private const val MODEL = "gemini-3.7-flash"

    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    suspend fun askGemini(
        userMessage: String
    ): Result<String> {

        return withContext(Dispatchers.IO) {

            try {

                val apiKey = BuildConfig.GEMINI_API_KEY

                if (apiKey.isBlank()) {
                    return@withContext Result.failure(
                        Exception("Gemini API key is missing.")
                    )
                }

                val url = URL(ENDPOINT)

                val connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty(
                    "x-goog-api-key",
                    apiKey
                )

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.connectTimeout = 15000
                connection.readTimeout = 30000

                connection.doOutput = true

                val requestBody =
                    JSONObject().apply {

                        put(
                            "contents",
                            org.json.JSONArray().apply {

                                put(
                                    JSONObject().apply {

                                        put(
                                            "parts",
                                            org.json.JSONArray().apply {

                                                put(
                                                    JSONObject().apply {
                                                        put(
                                                            "text",
                                                            userMessage
                                                        )
                                                    }
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }

                connection.outputStream.use { outputStream ->

                    outputStream.write(
                        requestBody
                            .toString()
                            .toByteArray(Charsets.UTF_8)
                    )
                }

                val responseCode =
                    connection.responseCode

                val responseText =
                    if (responseCode in 200..299) {

                        connection.inputStream
                            .bufferedReader()
                            .use { it.readText() }

                    } else {

                        connection.errorStream
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            ?: "Unknown Gemini API error"
                    }

                connection.disconnect()

                if (responseCode !in 200..299) {

                    return@withContext Result.failure(
                        Exception(
                            "Gemini API error $responseCode: $responseText"
                        )
                    )
                }

                val json =
                    JSONObject(responseText)

                val candidates =
                    json.optJSONArray("candidates")

                if (
                    candidates == null ||
                    candidates.length() == 0
                ) {

                    return@withContext Result.failure(
                        Exception(
                            "Gemini returned no response."
                        )
                    )
                }

                val text =
                    candidates
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                Result.success(text)

            } catch (e: Exception) {

                Result.failure(e)
            }
        }
    }
}