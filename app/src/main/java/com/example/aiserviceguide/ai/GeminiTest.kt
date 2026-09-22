package com.example.aiserviceguide.ai

import kotlinx.coroutines.runBlocking

fun main() {

    runBlocking {

        println("Testing Gemini API...")

        val result = GeminiService.askGemini(
            "Hello! I am testing the AI Service Guide Android project. " +
                    "Reply with a short sentence confirming that you can respond."
        )

        result.onSuccess { response ->

            println("================================")
            println("GEMINI RESPONSE:")
            println(response)
            println("================================")

        }.onFailure { error ->

            println("================================")
            println("GEMINI ERROR:")
            println(error.message)
            println("================================")

            error.printStackTrace()
        }
    }
}