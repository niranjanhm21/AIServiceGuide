package com.example.aiserviceguide.ai

import android.util.Log

/*
 * Temporary test for GeminiIntentAnalyzer.
 *
 * This file is only for testing the AI/NLP layer.
 *
 * We will remove/replace this testing approach after
 * the analyzer is successfully integrated into the app.
 */
object GeminiIntentTest {

    suspend fun runTest(): String {

        /*
         * These are the same services currently stored
         * in our Room database.
         */
        val availableServices = listOf(
            "Birth Certificate",
            "Death Certificate",
            "Income Certificate",
            "Residence / Domicile Certificate",
            "Caste Certificate",
            "Ration Card Services",
            "Driving Licence Services",
            "Vehicle Registration / RC Services",
            "Passport Application",
            "Aadhaar Enrolment / Update"
        )

        /*
         * Real natural-language citizen question.
         */
        val userMessage =
            "I need an income certificate but I don't have Aadhaar. How can I get it?"

        return try {

            /*
             * Send the message to GeminiIntentAnalyzer.
             */
            val result =
                GeminiIntentAnalyzer.analyze(
                    userMessage = userMessage,
                    availableServices = availableServices
                )

            result.fold(

                onSuccess = { intent ->

                    val output = """
                        
                        ===== GEMINI INTENT TEST =====
                        
                        User message:
                        $userMessage
                        
                        Service:
                        ${intent.service}
                        
                        Intent:
                        ${intent.intent}
                        
                        Document:
                        ${intent.document}
                        
                        Problem:
                        ${intent.problem}
                        
                        Desired action:
                        ${intent.desiredAction}
                        
                        Confidence:
                        ${intent.confidence}
                        
                        ==============================
                    """.trimIndent()

                    Log.d(
                        "GeminiIntentTest",
                        output
                    )

                    output
                },

                onFailure = { error ->

                    val output =
                        """
                        
                        ===== GEMINI TEST FAILED =====
                        
                        Error:
                        ${error.message}
                        
                        ===============================
                        """.trimIndent()

                    Log.e(
                        "GeminiIntentTest",
                        output,
                        error
                    )

                    output
                }
            )

        } catch (exception: Exception) {

            Log.e(
                "GeminiIntentTest",
                "Unexpected error",
                exception
            )

            "Gemini intent test failed: ${exception.message}"
        }
    }
}