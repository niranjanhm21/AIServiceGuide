package com.example.aiserviceguide.ai

import com.example.aiserviceguide.data.ChatMessage
import org.json.JSONObject

/**
 * Multilingual Natural Language Understanding for AI Service Guide.
 *
 * Supported:
 * - English
 * - Kannada
 * - Kannada + English mixed language
 *
 * IMPORTANT:
 * - Gemini does NOT decide government requirements.
 * - Room/database remains the source of truth.
 * - This class identifies service, intent, document and desired action.
 */
data class GeminiIntentResult(
    val service: String,
    val intent: String,
    val document: String,
    val problem: String,
    val desiredAction: String,
    val confidence: Double
)

object GeminiIntentAnalyzer {

    private val allowedIntents = setOf(
        "IDENTIFY_SERVICE",
        "APPLICATION_PROCEDURE",
        "DOCUMENT_REQUIREMENT",
        "MISSING_DOCUMENT",
        "DOCUMENT_ALTERNATIVE",
        "HOW_TO_OBTAIN_DOCUMENT",
        "WHERE_TO_OBTAIN_DOCUMENT",
        "OFFICE_LOCATION",
        "DEPARTMENT",
        "APPLICATION_PROBLEM",
        "REJECTION",
        "STATUS",
        "CHECKLIST",
        "START_ANOTHER_SERVICE",
        "GENERAL_HELP"
    )

    private val allowedActions = setOf(
        "NONE",
        "SHOW_REQUIREMENTS",
        "HOW_TO_OBTAIN",
        "WHERE_TO_OBTAIN",
        "FIND_ALTERNATIVE",
        "SHOW_CHECKLIST",
        "SHOW_OFFICE",
        "SOLVE_PROBLEM"
    )

    /**
     * Main NLU entry point.
     */
    suspend fun analyze(
        userMessage: String,
        availableServices: List<String>,
        currentService: String = "",
        currentDocument: String = ""
    ): Result<GeminiIntentResult> {

        if (userMessage.isBlank()) {
            return Result.failure(
                IllegalArgumentException("User message is empty.")
            )
        }

        /*
         * ------------------------------------------------
         * FIRST: LOCAL KANNADA / MIXED-LANGUAGE DETECTION
         * ------------------------------------------------
         *
         * This protects the app when Gemini does not correctly
         * understand a Kannada sentence.
         */
        val localResult =
            detectKannadaOrMixedIntent(
                userMessage = userMessage,
                availableServices = availableServices,
                currentService = currentService,
                currentDocument = currentDocument
            )

        if (localResult != null) {
            return Result.success(localResult)
        }

        val servicesText =
            availableServices.joinToString("\n") {
                "- $it"
            }

        /*
         * ------------------------------------------------
         * GEMINI MULTILINGUAL PROMPT
         * ------------------------------------------------
         */
        val prompt = """
You are the Natural Language Understanding component of an Android application
called "AI Service Guide".

The application supports:
1. English
2. Kannada
3. Kannada + English mixed language
4. English words written inside Kannada sentences

Your ONLY job is to understand the citizen's message and return structured JSON.

The application helps citizens prepare BEFORE visiting a government/public-service
office.

The application has a verified database.

IMPORTANT:
You must NEVER invent or guess:
- government requirements
- accepted documents
- alternatives
- offices
- fees
- legal rules
- procedures
- eligibility conditions

The database remains the source of truth.

AVAILABLE SERVICES IN THE DATABASE:
$servicesText

CURRENT CONVERSATION CONTEXT:
Current service:
${if (currentService.isBlank()) "NONE" else currentService}

Current document:
${if (currentDocument.isBlank()) "NONE" else currentDocument}

The citizen may speak or type in:
- English
- Kannada
- Kannada written naturally in Kannada script
- Kannada mixed with English
- English government terms inside Kannada sentences

Examples:

"ನನಗೆ income certificate ಬೇಕು"
means:
"I need an income certificate."

"ನನಗೆ ಇನ್ಕಮ್ ಸರ್ಟಿಫಿಕೇಟ್ ಬೇಕು"
means:
"I need an income certificate."

"ನನಗೆ ಆದಾಯ ಪ್ರಮಾಣ ಪತ್ರ ಬೇಕು"
means:
"I need an income certificate."

"ನನ್ನ ಬಳಿ address proof ಇಲ್ಲ"
means:
"I don't have address proof."

"ನನ್ನ ಬಳಿ ವಿಳಾಸದ ಪುರಾವೆ ಇಲ್ಲ"
means:
"I don't have proof of address."

"ನನಗೆ caste certificate ಬೇಕು"
means:
"I need a caste certificate."

"ನನಗೆ ಜಾತಿ ಪ್ರಮಾಣ ಪತ್ರ ಬೇಕು"
means:
"I need a caste certificate."

"ಇದು ಹೇಗೆ ಪಡೆಯುವುದು?"
means:
"How do I get it?"

"ಇದನ್ನು ಎಲ್ಲಿ ಪಡೆಯಬಹುದು?"
means:
"Where can I get it?"

"ಬೇರೆ document ಬಳಸಬಹುದಾ?"
means:
"Can I use another document?"

Do NOT require the user to repeat the service or document when it already
exists in CURRENT CONVERSATION CONTEXT.

For example:

Current service = Income Certificate
Current document = Proof of Address

User:
"ಇದನ್ನು ಎಲ್ಲಿ ಪಡೆಯಬಹುದು?"

The correct interpretation is:
service = Income Certificate
document = Proof of Address
intent = WHERE_TO_OBTAIN_DOCUMENT
desiredAction = WHERE_TO_OBTAIN

USER MESSAGE:
"$userMessage"

Return ONLY valid JSON.

JSON schema:

{
  "service": "exact service name from available services or empty string",
  "intent": "one allowed intent",
  "document": "document mentioned or empty string",
  "problem": "short description of problem or empty string",
  "desiredAction": "one allowed desired action",
  "confidence": 0.0
}

ALLOWED INTENTS:
${allowedIntents.joinToString(", ")}

ALLOWED DESIRED ACTIONS:
${allowedActions.joinToString(", ")}

INTENT RULES:

IDENTIFY_SERVICE:
Use when the citizen mainly says what government service they need.

DOCUMENT_REQUIREMENT:
Use when asking what documents are required.

MISSING_DOCUMENT:
Use when the citizen says they do not have, lost, cannot find,
or are missing a document.

HOW_TO_OBTAIN_DOCUMENT:
Use when asking how to get a document.

WHERE_TO_OBTAIN_DOCUMENT:
Use when asking where to get a document.

DOCUMENT_ALTERNATIVE:
Use when explicitly asking whether another document can be used.

CHECKLIST:
Use when asking for a final checklist.

APPLICATION_PROBLEM:
Use for problems during application.

REJECTION:
Use when an application was rejected or returned.

STATUS:
Use when asking application status.

OFFICE_LOCATION:
Use when asking where to visit.

DEPARTMENT:
Use when asking which department handles the service.

START_ANOTHER_SERVICE:
Use when switching to another service.

GENERAL_HELP:
Use for unclear questions.

DESIRED ACTION RULES:

If asking how to obtain:
desiredAction = HOW_TO_OBTAIN

If asking where to obtain:
desiredAction = WHERE_TO_OBTAIN

If asking for another/alternative document:
desiredAction = FIND_ALTERNATIVE

If asking required documents:
desiredAction = SHOW_REQUIREMENTS

If asking checklist:
desiredAction = SHOW_CHECKLIST

If asking where to visit:
desiredAction = SHOW_OFFICE

Otherwise:
desiredAction = NONE

CONTEXT RULE:

If CURRENT CONVERSATION CONTEXT contains service or document and the user
refers to:
"it"
"this"
"that"
"this document"
"that document"
"ಇದು"
"ಅದು"
"ಇದನ್ನು"
"ಅದನ್ನು"
"ಈ document"
"ಆ document"

reuse the existing conversation context.

SERVICE NORMALIZATION:

If the user says:
income certificate
income cert
income proof certificate
ಆದಾಯ ಪ್ರಮಾಣ ಪತ್ರ
ಇನ್ಕಮ್ ಸರ್ಟಿಫಿಕೇಟ್

map to the available service whose name corresponds to Income Certificate.

If the user says:
caste certificate
caste cert
ಜಾತಿ ಪ್ರಮಾಣ ಪತ್ರ
ಜಾತಿ ಸರ್ಟಿಫಿಕೇಟ್

map to Caste Certificate.

If the user says:
birth certificate
birth cert
ಜನನ ಪ್ರಮಾಣ ಪತ್ರ
ಜನನ ಸರ್ಟಿಫಿಕೇಟ್

map to Birth Certificate.

If the user says:
death certificate
ಮರಣ ಪ್ರಮಾಣ ಪತ್ರ

map to Death Certificate.

If the user says:
residence certificate
residential certificate
ನಿವಾಸ ಪ್ರಮಾಣ ಪತ್ರ
ವಾಸಸ್ಥಳ ಪ್ರಮಾಣ ಪತ್ರ

map to Residence Certificate.

If the user says:
ration card
ಪಡಿತರ ಚೀಟಿ
ರೇಷನ್ ಕಾರ್ಡ್

map to Ration Card.

If the user says:
driving licence
driving license
ಚಾಲನಾ ಪರವಾನಗಿ
ಡ್ರೈವಿಂಗ್ ಲೈಸೆನ್ಸ್

map to Driving Licence.

If the user says:
passport
ಪಾಸ್ಪೋರ್ಟ್

map to Passport.

DOCUMENT NORMALIZATION:

Aadhaar:
- Aadhaar
- Aadhar
- Aadhaar card
- ಆಧಾರ್
- ಆಧಾರ್ ಕಾರ್ಡ್

Proof of Address:
- address proof
- proof of address
- address document
- ವಿಳಾಸದ ಪುರಾವೆ
- ವಿಳಾಸ ಪ್ರಮಾಣ

Proof of Identity:
- identity proof
- ID proof
- proof of identity
- ಗುರುತಿನ ಪುರಾವೆ
- ಗುರುತಿನ ಚೀಟಿ

Income Evidence:
- income proof
- income evidence
- ಆದಾಯದ ಪುರಾವೆ
- ಆದಾಯ ಪ್ರಮಾಣ

Caste/Community Evidence:
- caste proof
- caste evidence
- ಜಾತಿ ಪ್ರಮಾಣ
- ಜಾತಿಯ ಪುರಾವೆ

Birth Certificate:
- birth certificate
- ಜನನ ಪ್ರಮಾಣ ಪತ್ರ

Death Certificate:
- death certificate
- ಮರಣ ಪ್ರಮಾಣ ಪತ್ರ

Return confidence between 0.0 and 1.0.

Return ONLY the JSON object.
""".trimIndent()

        return try {

            val rawResponse =
                GeminiService
                    .askGemini(prompt)
                    .getOrThrow()

            val jsonText =
                cleanJsonResponse(rawResponse)

            val json =
                JSONObject(jsonText)

            val service =
                json
                    .optString("service", "")
                    .trim()

            val rawIntent =
                json
                    .optString(
                        "intent",
                        "GENERAL_HELP"
                    )
                    .trim()
                    .uppercase()

            val document =
                json
                    .optString("document", "")
                    .trim()

            val contextualDocument =
                if (
                    document.isBlank() &&
                    currentDocument.isNotBlank()
                ) {
                    currentDocument.trim()
                } else {
                    document
                }

            val problem =
                json
                    .optString("problem", "")
                    .trim()

            val rawAction =
                json
                    .optString(
                        "desiredAction",
                        "NONE"
                    )
                    .trim()
                    .uppercase()

            val confidence =
                json
                    .optDouble(
                        "confidence",
                        0.0
                    )
                    .coerceIn(
                        0.0,
                        1.0
                    )

            val normalizedDocument =
                normalizeDocumentName(
                    document =
                        contextualDocument,
                    userMessage =
                        userMessage
                )

            val normalizedIntent =
                normalizeIntent(
                    intent =
                        rawIntent,
                    userMessage =
                        userMessage,
                    document =
                        normalizedDocument
                )

            val normalizedAction =
                normalizeDesiredAction(
                    userMessage =
                        userMessage,
                    currentAction =
                        rawAction,
                    intent =
                        normalizedIntent
                )

            val finalIntent =
                if (
                    normalizedDocument.isNotBlank() &&
                    containsMissingDocumentLanguage(
                        userMessage
                    )
                ) {
                    "MISSING_DOCUMENT"
                } else {
                    normalizedIntent
                }

            val finalAction =
                if (
                    finalIntent ==
                    "MISSING_DOCUMENT" &&
                    normalizedAction == "NONE" &&
                    containsNextStepLanguage(
                        userMessage
                    )
                ) {
                    "HOW_TO_OBTAIN"
                } else {
                    normalizedAction
                }

            val normalizedService =
                normalizeServiceName(
                    service =
                        service,
                    availableServices =
                        availableServices,
                    userMessage =
                        userMessage
                ).ifBlank {
                    currentService.trim()
                }

            Result.success(
                GeminiIntentResult(
                    service =
                        normalizedService,

                    intent =
                        finalIntent,

                    document =
                        normalizedDocument,

                    problem =
                        if (
                            problem.isNotBlank()
                        ) {
                            problem
                        } else if (
                            finalIntent ==
                            "MISSING_DOCUMENT"
                        ) {
                            "The citizen does not have ${normalizedDocument}."
                        } else {
                            ""
                        },

                    desiredAction =
                        finalAction,

                    confidence =
                        confidence
                )
            )

        } catch (exception: Exception) {

            Result.failure(
                exception
            )
        }
    }

    /**
     * Local Kannada + mixed-language detection.
     *
     * This is deliberately conservative.
     * It identifies the user's intent but does not decide
     * whether a document is legally required.
     */
    private fun detectKannadaOrMixedIntent(
        userMessage: String,
        availableServices: List<String>,
        currentService: String,
        currentDocument: String
    ): GeminiIntentResult? {

        val text =
            userMessage
                .lowercase()
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()

        val hasKannada =
            text.any { character ->
                character in '\u0C80'..'\u0CFF'
            }

        if (!hasKannada) {
            return null
        }

        /*
         * -----------------------------------------------
         * SERVICE DETECTION
         * -----------------------------------------------
         */
        val service =
            detectKannadaService(
                text =
                    text,
                availableServices =
                    availableServices
            ) ?: currentService.takeIf {
                it.isNotBlank()
            }

        /*
         * -----------------------------------------------
         * DOCUMENT DETECTION
         * -----------------------------------------------
         */
        val document =
            detectKannadaDocument(
                text =
                    text
            ) ?: currentDocument.takeIf {
                it.isNotBlank()
            }

        /*
         * -----------------------------------------------
         * INTENT DETECTION
         * -----------------------------------------------
         */

        val asksAlternative =
            containsAny(
                text,
                "ಬೇರೆ document",
                "ಬೇರೆ ಡಾಕ್ಯುಮೆಂಟ್",
                "ಬೇರೆ ದಾಖಲೆ",
                "ಪರ್ಯಾಯ",
                "ಬದಲಿ",
                "ಬೇರೆ ಪ್ರಮಾಣ",
                "ಇನ್ನೊಂದು document",
                "ಇನ್ನೊಂದು ಡಾಕ್ಯುಮೆಂಟ್",
                "another document",
                "alternative",
                "substitute"
            )

        if (
            asksAlternative &&
            document != null
        ) {
            return GeminiIntentResult(
                service =
                    service ?: "",
                intent =
                    "DOCUMENT_ALTERNATIVE",
                document =
                    document,
                problem =
                    "",
                desiredAction =
                    "FIND_ALTERNATIVE",
                confidence =
                    0.95
            )
        }

        val asksWhere =
            containsAny(
                text,
                "ಎಲ್ಲಿ ಪಡೆಯ",
                "ಎಲ್ಲಿ ಸಿಗ",
                "ಎಲ್ಲಿ ದೊರೆಯ",
                "ಎಲ್ಲಿ ಪಡೆಯಬಹುದು",
                "ಯಾವ ಕಚೇರಿ",
                "ಯಾವ ಆಫೀಸ್",
                "where can i get",
                "where do i get",
                "where to get"
            )

        if (
            asksWhere &&
            document != null
        ) {
            return GeminiIntentResult(
                service =
                    service ?: "",
                intent =
                    "WHERE_TO_OBTAIN_DOCUMENT",
                document =
                    document,
                problem =
                    "",
                desiredAction =
                    "WHERE_TO_OBTAIN",
                confidence =
                    0.95
            )
        }

        val asksHow =
            containsAny(
                text,
                "ಹೇಗೆ ಪಡೆಯ",
                "ಹೇಗೆ ಪಡೆಯಬಹುದು",
                "ಹೇಗೆ ಸಿಗುತ್ತದೆ",
                "ಹೇಗೆ ಪಡೆಯುವುದು",
                "ಹೇಗೆ ಮಾಡುವುದು",
                "ಯಾವ ರೀತಿ ಪಡೆಯ",
                "how do i get",
                "how can i get",
                "how to get"
            )

        if (
            asksHow &&
            document != null
        ) {
            return GeminiIntentResult(
                service =
                    service ?: "",
                intent =
                    "HOW_TO_OBTAIN_DOCUMENT",
                document =
                    document,
                problem =
                    "",
                desiredAction =
                    "HOW_TO_OBTAIN",
                confidence =
                    0.95
            )
        }

        val missing =
            containsAny(
                text,
                "ಇಲ್ಲ",
                "ಇಲ್ಲದೆ",
                "ನನ್ನ ಬಳಿ ಇಲ್ಲ",
                "ನನ್ನ ಹತ್ತಿರ ಇಲ್ಲ",
                "ಇಲ್ಲವಾಗಿದೆ",
                "ಕಳೆದುಹೋಗಿದೆ",
                "ಕಳೆದುಕೊಂಡಿದ್ದೇನೆ",
                "ಬೇಕಿದೆ ಆದರೆ ಇಲ್ಲ",
                "ನನ್ನ ಬಳಿ ... ಇಲ್ಲ",
                "don't have",
                "do not have",
                "missing",
                "without"
            )

        if (
            missing &&
            document != null
        ) {
            return GeminiIntentResult(
                service =
                    service ?: "",
                intent =
                    "MISSING_DOCUMENT",
                document =
                    document,
                problem =
                    "The citizen does not have $document.",
                desiredAction =
                    if (
                        containsAny(
                            text,
                            "ಹೇಗೆ",
                            "ಏನು ಮಾಡಬೇಕು",
                            "ಏನು ಮಾಡಲಿ",
                            "ಮುಂದೆ ಏನು"
                        )
                    ) {
                        "HOW_TO_OBTAIN"
                    } else {
                        "NONE"
                    },
                confidence =
                    0.96
            )
        }

        /*
         * -----------------------------------------------
         * SERVICE REQUEST
         * -----------------------------------------------
         */
        if (
            service != null &&
            containsAny(
                text,
                "ಬೇಕು",
                "ಬೇಕಾಗಿದೆ",
                "ಬೇಕಿದೆ",
                "ಬೇಕಾಗುತ್ತದೆ",
                "ಪಡೆಯಬೇಕು",
                "ಪಡೆಯಲು",
                "ಅಪ್ಲೈ",
                "apply",
                "need",
                "want"
            )
        ) {
            return GeminiIntentResult(
                service =
                    service,
                intent =
                    "IDENTIFY_SERVICE",
                document =
                    document ?: "",
                problem =
                    "",
                desiredAction =
                    "NONE",
                confidence =
                    0.96
            )
        }

        /*
         * If Kannada is present and we know the service,
         * do not immediately fail.
         */
        if (
            service != null
        ) {
            return GeminiIntentResult(
                service =
                    service,
                intent =
                    "IDENTIFY_SERVICE",
                document =
                    document ?: "",
                problem =
                    "",
                desiredAction =
                    "NONE",
                confidence =
                    0.88
            )
        }

        return null
    }

    private fun detectKannadaService(
        text: String,
        availableServices: List<String>
    ): String? {

        fun find(
            vararg keywords: String
        ): String? {

            val matched =
                keywords.any {
                    text.contains(it)
                }

            if (!matched) {
                return null
            }

            return availableServices.firstOrNull {
                val name =
                    it.lowercase()

                keywords.any { keyword ->
                    name.contains(
                        keyword
                    )
                }
            } ?: availableServices.firstOrNull {
                val name =
                    it.lowercase()

                when {
                    keywords.contains("income") ->
                        name.contains("income")

                    keywords.contains("caste") ->
                        name.contains("caste")

                    keywords.contains("birth") ->
                        name.contains("birth")

                    keywords.contains("death") ->
                        name.contains("death")

                    keywords.contains("ration") ->
                        name.contains("ration")

                    keywords.contains("passport") ->
                        name.contains("passport")

                    keywords.contains("driving") ->
                        name.contains("driving")

                    keywords.contains("residence") ->
                        name.contains("residence") ||
                                name.contains("domicile")

                    else ->
                        false
                }
            }
        }

        return when {

            containsAny(
                text,
                "income certificate",
                "income cert",
                "ಇನ್ಕಮ್ ಸರ್ಟಿಫಿಕೇಟ್",
                "ಇನ್ಕಮ್ ಪ್ರಮಾಣ ಪತ್ರ",
                "ಆದಾಯ ಪ್ರಮಾಣ ಪತ್ರ",
                "ಆದಾಯದ ಪ್ರಮಾಣ ಪತ್ರ",
                "ಆದಾಯ ಪ್ರಮಾಣ"
            ) ->
                find("income")

            containsAny(
                text,
                "caste certificate",
                "caste cert",
                "ಜಾತಿ ಪ್ರಮಾಣ ಪತ್ರ",
                "ಜಾತಿ ಪ್ರಮಾಣಪತ್ರ",
                "ಜಾತಿ ಸರ್ಟಿಫಿಕೇಟ್",
                "ಜಾತಿ ಪ್ರಮಾಣ"
            ) ->
                find("caste")

            containsAny(
                text,
                "birth certificate",
                "birth cert",
                "ಜನನ ಪ್ರಮಾಣ ಪತ್ರ",
                "ಜನನ ಪ್ರಮಾಣಪತ್ರ",
                "ಜನನ ಸರ್ಟಿಫಿಕೇಟ್"
            ) ->
                find("birth")

            containsAny(
                text,
                "death certificate",
                "death cert",
                "ಮರಣ ಪ್ರಮಾಣ ಪತ್ರ",
                "ಮರಣ ಪ್ರಮಾಣಪತ್ರ"
            ) ->
                find("death")

            containsAny(
                text,
                "residence certificate",
                "residential certificate",
                "domicile",
                "ನಿವಾಸ ಪ್ರಮಾಣ ಪತ್ರ",
                "ನಿವಾಸ ಪ್ರಮಾಣಪತ್ರ",
                "ವಾಸಸ್ಥಳ ಪ್ರಮಾಣ ಪತ್ರ"
            ) ->
                find("residence")

            containsAny(
                text,
                "ration card",
                "ರೇಷನ್ ಕಾರ್ಡ್",
                "ರೇಷನ್ ಕಾರ್ಡ",
                "ಪಡಿತರ ಚೀಟಿ"
            ) ->
                find("ration")

            containsAny(
                text,
                "driving licence",
                "driving license",
                "ಡ್ರೈವಿಂಗ್ ಲೈಸೆನ್ಸ್",
                "ಚಾಲನಾ ಪರವಾನಗಿ"
            ) ->
                find("driving")

            containsAny(
                text,
                "passport",
                "ಪಾಸ್ಪೋರ್ಟ್"
            ) ->
                find("passport")

            else ->
                null
        }
    }

    private fun detectKannadaDocument(
        text: String
    ): String? {

        return when {

            containsAny(
                text,
                "aadhaar",
                "aadhar",
                "ಆಧಾರ್",
                "ಆಧಾರ್ ಕಾರ್ಡ್",
                "ಆಧಾರ್ ಕಾರ್ಡ"
            ) ->
                "Aadhaar / e-Aadhaar"

            containsAny(
                text,
                "address proof",
                "proof of address",
                "address document",
                "ವಿಳಾಸದ ಪುರಾವೆ",
                "ವಿಳಾಸ ಪ್ರಮಾಣ",
                "ವಿಳಾಸದ ದಾಖಲೆ",
                "ವಿಳಾಸದ ಪುರಾವೆ"
            ) ->
                "Proof of Address"

            containsAny(
                text,
                "identity proof",
                "proof of identity",
                "id proof",
                "ಗುರುತಿನ ಪುರಾವೆ",
                "ಗುರುತಿನ ಚೀಟಿ",
                "ಗುರುತಿನ ದಾಖಲೆ"
            ) ->
                "Proof of Identity"

            containsAny(
                text,
                "income proof",
                "income evidence",
                "ಆದಾಯದ ಪುರಾವೆ",
                "ಆದಾಯ ಪ್ರಮಾಣ"
            ) ->
                "Income Evidence"

            containsAny(
                text,
                "caste proof",
                "caste evidence",
                "ಜಾತಿಯ ಪುರಾವೆ",
                "ಜಾತಿ ಪ್ರಮಾಣ"
            ) ->
                "Caste/Community Evidence"

            containsAny(
                text,
                "birth certificate",
                "ಜನನ ಪ್ರಮಾಣ ಪತ್ರ",
                "ಜನನ ಪ್ರಮಾಣಪತ್ರ"
            ) ->
                "Birth Certificate / proof of date of birth"

            containsAny(
                text,
                "death certificate",
                "ಮರಣ ಪ್ರಮಾಣ ಪತ್ರ",
                "ಮರಣ ಪ್ರಮಾಣಪತ್ರ"
            ) ->
                "Death Certificate"

            else ->
                null
        }
    }

    /**
     * Generates a natural citizen-facing response from verified facts.
     *
     * The response language follows the citizen's language:
     * Kannada -> Kannada
     * English -> English
     * Mixed -> simple mixed/appropriate language
     */
    suspend fun generateCitizenResponse(
        userMessage: String,
        verifiedFacts: String,
        fallbackResponse: String
    ): String {

        if (verifiedFacts.isBlank()) {
            return fallbackResponse
        }

        val hasKannada =
            userMessage.any {
                it in '\u0C80'..'\u0CFF'
            }

        val languageInstruction =
            if (hasKannada) {
                """
Respond primarily in Kannada.
You may keep official service names, document names,
URLs and technical terms in English when that makes them clearer.
Use simple Kannada that an ordinary citizen can understand.
Do not translate or change official names incorrectly.
""".trimIndent()
            } else {
                """
Respond in simple English.
""".trimIndent()
            }

        val prompt = """
You are the response-generation component of an Android application
called "AI Service Guide".

$languageInstruction

The VERIFIED DATABASE FACTS below are the ONLY source of truth.

Do NOT:
- invent requirements
- invent documents
- invent alternatives
- invent offices
- invent fees
- invent URLs
- invent legal rules
- invent deadlines
- change database facts

Explain only the supplied verified information.

USER MESSAGE:
$userMessage

VERIFIED DATABASE FACTS:
$verifiedFacts

FALLBACK RESPONSE:
$fallbackResponse

Return a concise, helpful citizen-facing response.
""".trimIndent()

        return try {

            GeminiService
                .askGemini(prompt)
                .getOrThrow()
                .trim()
                .ifBlank {
                    fallbackResponse
                }

        } catch (
            exception: Exception
        ) {

            fallbackResponse
        }
    }

    private fun normalizeDocumentName(
        document: String,
        userMessage: String
    ): String {

        val aliases =
            listOf(

                "Aadhaar / e-Aadhaar" to listOf(
                    "aadhaar",
                    "aadhar",
                    "aadhaar card",
                    "aadhar card",
                    "e-aadhaar",
                    "e aadhar",
                    "ಆಧಾರ್",
                    "ಆಧಾರ್ ಕಾರ್ಡ್"
                ),

                "Proof of Address" to listOf(
                    "proof of address",
                    "address proof",
                    "address document",
                    "address certificate",
                    "ವಿಳಾಸದ ಪುರಾವೆ",
                    "ವಿಳಾಸ ಪ್ರಮಾಣ",
                    "ವಿಳಾಸದ ದಾಖಲೆ"
                ),

                "Proof of Identity" to listOf(
                    "proof of identity",
                    "identity proof",
                    "id proof",
                    "identity document",
                    "ಗುರುತಿನ ಪುರಾವೆ",
                    "ಗುರುತಿನ ಚೀಟಿ",
                    "ಗುರುತಿನ ದಾಖಲೆ"
                ),

                "Photograph" to listOf(
                    "photograph",
                    "photo",
                    "passport photo",
                    "passport size photo",
                    "ಛಾಯಾಚಿತ್ರ"
                ),

                "Birth Certificate / proof of date of birth" to listOf(
                    "birth certificate",
                    "date of birth proof",
                    "dob proof",
                    "proof of date of birth",
                    "ಜನನ ಪ್ರಮಾಣ ಪತ್ರ",
                    "ಜನನ ಪ್ರಮಾಣಪತ್ರ"
                ),

                "Income Evidence" to listOf(
                    "income evidence",
                    "income proof",
                    "proof of income",
                    "income document",
                    "ಆದಾಯದ ಪುರಾವೆ",
                    "ಆದಾಯ ಪ್ರಮಾಣ"
                ),

                "Caste/Community Evidence" to listOf(
                    "caste evidence",
                    "caste proof",
                    "caste certificate",
                    "community evidence",
                    "community proof",
                    "ಜಾತಿ ಪ್ರಮಾಣ",
                    "ಜಾತಿಯ ಪುರಾವೆ"
                ),

                "Existing Driving Licence" to listOf(
                    "driving licence",
                    "driving license",
                    "driver's licence",
                    "drivers licence",
                    "dl",
                    "ಚಾಲನಾ ಪರವಾನಗಿ",
                    "ಡ್ರೈವಿಂಗ್ ಲೈಸೆನ್ಸ್"
                ),

                "Ration Card / family details" to listOf(
                    "ration card",
                    "family details",
                    "ration details",
                    "ರೇಷನ್ ಕಾರ್ಡ್",
                    "ಪಡಿತರ ಚೀಟಿ"
                )
            )

        val supplied =
            document
                .trim()
                .lowercase()

        if (
            supplied.isNotBlank()
        ) {

            aliases.firstOrNull {
                    (_, names) ->
                names.any {
                    supplied.contains(
                        it
                    )
                }
            }?.let {
                    (canonical, _) ->
                return canonical
            }
        }

        val text =
            userMessage.lowercase()

        aliases.firstOrNull {
                (_, names) ->
            names.any {
                text.contains(
                    it
                )
            }
        }?.let {
                (canonical, _) ->
            return canonical
        }

        return document.trim()
    }

    private fun containsMissingDocumentLanguage(
        userMessage: String
    ): Boolean {

        val text =
            userMessage.lowercase()

        return containsAny(
            text,
            "don't have",
            "do not have",
            "dont have",
            "missing",
            "lost my",
            "lost the",
            "i have no",
            "i don't have",
            "i do not have",
            "not having",
            "without",
            "ಇಲ್ಲ",
            "ನನ್ನ ಬಳಿ ಇಲ್ಲ",
            "ನನ್ನ ಹತ್ತಿರ ಇಲ್ಲ",
            "ಕಳೆದುಹೋಗಿದೆ",
            "ಕಳೆದುಕೊಂಡಿದ್ದೇನೆ"
        )
    }

    private fun containsNextStepLanguage(
        userMessage: String
    ): Boolean {

        val text =
            userMessage.lowercase()

        return containsAny(
            text,
            "what should i do",
            "what do i do",
            "what can i do",
            "what next",
            "next step",
            "how do i get",
            "how can i get",
            "how to get",
            "how do i obtain",
            "how can i obtain",
            "how to obtain",
            "ಹೇಗೆ ಪಡೆಯ",
            "ಹೇಗೆ ಪಡೆಯುವುದು",
            "ಏನು ಮಾಡಬೇಕು",
            "ಏನು ಮಾಡಲಿ",
            "ಮುಂದೆ ಏನು"
        )
    }

    private fun normalizeIntent(
        intent: String,
        userMessage: String,
        document: String
    ): String {

        val text =
            userMessage.lowercase()

        if (
            containsAny(
                text,
                "alternative",
                "another document",
                "another proof",
                "other document",
                "substitute",
                "instead of",
                "ಬೇರೆ document",
                "ಬೇರೆ ಡಾಕ್ಯುಮೆಂಟ್",
                "ಬೇರೆ ದಾಖಲೆ",
                "ಪರ್ಯಾಯ",
                "ಬದಲಿ"
            ) &&
            document.isNotBlank()
        ) {
            return "DOCUMENT_ALTERNATIVE"
        }

        if (
            containsAny(
                text,
                "where can i get",
                "where do i get",
                "where to get",
                "which office",
                "ಎಲ್ಲಿ ಪಡೆಯ",
                "ಎಲ್ಲಿ ಸಿಗ",
                "ಎಲ್ಲಿ ದೊರೆಯ",
                "ಯಾವ ಕಚೇರಿ"
            ) &&
            document.isNotBlank()
        ) {
            return "WHERE_TO_OBTAIN_DOCUMENT"
        }

        if (
            containsAny(
                text,
                "how do i get",
                "how can i get",
                "how to get",
                "how do i obtain",
                "how can i obtain",
                "how to obtain",
                "ಹೇಗೆ ಪಡೆಯ",
                "ಹೇಗೆ ಪಡೆಯುವುದು",
                "ಹೇಗೆ ಸಿಗುತ್ತದೆ"
            ) &&
            document.isNotBlank()
        ) {
            return "HOW_TO_OBTAIN_DOCUMENT"
        }

        if (
            containsMissingDocumentLanguage(
                userMessage
            ) &&
            document.isNotBlank()
        ) {
            return "MISSING_DOCUMENT"
        }

        return if (
            intent in allowedIntents
        ) {
            intent
        } else {
            "GENERAL_HELP"
        }
    }

    private fun normalizeDesiredAction(
        userMessage: String,
        currentAction: String,
        intent: String
    ): String {

        val text =
            userMessage.lowercase()

        if (
            containsAny(
                text,
                "alternative",
                "another document",
                "another proof",
                "substitute",
                "ಬೇರೆ document",
                "ಬೇರೆ ಡಾಕ್ಯುಮೆಂಟ್",
                "ಪರ್ಯಾಯ",
                "ಬದಲಿ"
            )
        ) {
            return "FIND_ALTERNATIVE"
        }

        if (
            containsAny(
                text,
                "where can i get",
                "where do i get",
                "where to get",
                "which office",
                "ಎಲ್ಲಿ ಪಡೆಯ",
                "ಎಲ್ಲಿ ಸಿಗ",
                "ಎಲ್ಲಿ ಪಡೆಯಬಹುದು",
                "ಯಾವ ಕಚೇರಿ"
            )
        ) {
            return "WHERE_TO_OBTAIN"
        }

        if (
            containsAny(
                text,
                "how do i get",
                "how can i get",
                "how to get",
                "how do i obtain",
                "how can i obtain",
                "how to obtain",
                "ಹೇಗೆ ಪಡೆಯ",
                "ಹೇಗೆ ಪಡೆಯುವುದು",
                "ಹೇಗೆ ಸಿಗುತ್ತದೆ"
            )
        ) {
            return "HOW_TO_OBTAIN"
        }

        if (
            containsAny(
                text,
                "checklist",
                "complete list",
                "final list",
                "what should i carry",
                "what should i take",
                "ಚೆಕ್‌ಲಿಸ್ಟ್",
                "ಪಟ್ಟಿ",
                "ಏನು ತೆಗೆದುಕೊಂಡು ಹೋಗಬೇಕು"
            )
        ) {
            return "SHOW_CHECKLIST"
        }

        if (
            containsAny(
                text,
                "what documents",
                "documents required",
                "documents do i need",
                "what papers",
                "required documents",
                "which documents",
                "ಯಾವ ದಾಖಲೆಗಳು",
                "ಯಾವ document",
                "ಯಾವ ಡಾಕ್ಯುಮೆಂಟ್",
                "ಯಾವ ದಾಖಲೆ ಬೇಕು"
            )
        ) {
            return "SHOW_REQUIREMENTS"
        }

        return when (intent) {

            "HOW_TO_OBTAIN_DOCUMENT" ->
                "HOW_TO_OBTAIN"

            "WHERE_TO_OBTAIN_DOCUMENT" ->
                "WHERE_TO_OBTAIN"

            "DOCUMENT_ALTERNATIVE" ->
                "FIND_ALTERNATIVE"

            "DOCUMENT_REQUIREMENT" ->
                "SHOW_REQUIREMENTS"

            "CHECKLIST" ->
                "SHOW_CHECKLIST"

            "OFFICE_LOCATION" ->
                "SHOW_OFFICE"

            "APPLICATION_PROBLEM",
            "REJECTION" ->
                "SOLVE_PROBLEM"

            else ->
                if (
                    currentAction in allowedActions
                ) {
                    currentAction
                } else {
                    "NONE"
                }
        }
    }

    private fun normalizeServiceName(
        service: String,
        availableServices: List<String>,
        userMessage: String
    ): String {

        if (
            service.isNotBlank()
        ) {

            availableServices.firstOrNull {
                it.equals(
                    service,
                    ignoreCase = true
                )
            }?.let {
                return it
            }

            availableServices.firstOrNull {
                it.contains(
                    service,
                    ignoreCase = true
                ) ||
                        service.contains(
                            it,
                            ignoreCase = true
                        )
            }?.let {
                return it
            }
        }

        val text =
            userMessage.lowercase()

        fun find(
            vararg aliases: String
        ): String {

            return availableServices.firstOrNull {
                val name =
                    it.lowercase()

                aliases.any {
                    name.contains(it)
                }
            } ?: ""
        }

        return when {

            containsAny(
                text,
                "income certificate",
                "income cert",
                "ಇನ್ಕಮ್ ಸರ್ಟಿಫಿಕೇಟ್",
                "ಇನ್ಕಮ್ ಪ್ರಮಾಣ ಪತ್ರ",
                "ಆದಾಯ ಪ್ರಮಾಣ ಪತ್ರ",
                "ಆದಾಯ ಪ್ರಮಾಣ"
            ) ->
                find("income")

            containsAny(
                text,
                "caste certificate",
                "caste cert",
                "ಜಾತಿ ಪ್ರಮಾಣ ಪತ್ರ",
                "ಜಾತಿ ಪ್ರಮಾಣಪತ್ರ",
                "ಜಾತಿ ಸರ್ಟಿಫಿಕೇಟ್"
            ) ->
                find("caste")

            containsAny(
                text,
                "birth certificate",
                "birth cert",
                "ಜನನ ಪ್ರಮಾಣ ಪತ್ರ",
                "ಜನನ ಪ್ರಮಾಣಪತ್ರ"
            ) ->
                find("birth")

            containsAny(
                text,
                "death certificate",
                "death cert",
                "ಮರಣ ಪ್ರಮಾಣ ಪತ್ರ",
                "ಮರಣ ಪ್ರಮಾಣಪತ್ರ"
            ) ->
                find("death")

            containsAny(
                text,
                "residence certificate",
                "residential certificate",
                "domicile",
                "ನಿವಾಸ ಪ್ರಮಾಣ ಪತ್ರ",
                "ವಾಸಸ್ಥಳ ಪ್ರಮಾಣ ಪತ್ರ"
            ) ->
                find(
                    "residence",
                    "domicile"
                )

            containsAny(
                text,
                "ration card",
                "ರೇಷನ್ ಕಾರ್ಡ್",
                "ಪಡಿತರ ಚೀಟಿ"
            ) ->
                find("ration")

            containsAny(
                text,
                "driving licence",
                "driving license",
                "ಚಾಲನಾ ಪರವಾನಗಿ",
                "ಡ್ರೈವಿಂಗ್ ಲೈಸೆನ್ಸ್"
            ) ->
                find("driving")

            containsAny(
                text,
                "passport",
                "ಪಾಸ್ಪೋರ್ಟ್"
            ) ->
                find("passport")

            else ->
                ""
        }
    }

    private fun containsAny(
        text: String,
        vararg phrases: String
    ): Boolean {

        return phrases.any {
            text.contains(it)
        }
    }

    private fun cleanJsonResponse(
        response: String
    ): String {

        var text =
            response.trim()

        if (
            text.startsWith("```")
        ) {

            text =
                text
                    .removePrefix(
                        "```json"
                    )
                    .removePrefix(
                        "```JSON"
                    )
                    .removePrefix(
                        "```"
                    )
                    .trim()

            if (
                text.endsWith("```")
            ) {

                text =
                    text
                        .removeSuffix("```")
                        .trim()
            }
        }

        val firstBrace =
            text.indexOf('{')

        val lastBrace =
            text.lastIndexOf('}')

        if (
            firstBrace >= 0 &&
            lastBrace > firstBrace
        ) {

            text =
                text.substring(
                    firstBrace,
                    lastBrace + 1
                )
        }

        return text.trim()
    }
}