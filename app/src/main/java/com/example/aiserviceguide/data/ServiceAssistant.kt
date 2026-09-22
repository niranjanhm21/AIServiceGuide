package com.example.aiserviceguide.data

import com.example.aiserviceguide.ai.GeminiIntentAnalyzer

object ServiceAssistant {

    /*
 * ------------------------------------------------
 * AI-POWERED MESSAGE PROCESSING
 * ------------------------------------------------
 *
 * Gemini understands the citizen's natural-language
 * request first.
 *
 * Room database remains the source of truth.
 *
 * If Gemini fails, we fall back to the existing
 * rule-based processMessage() function.
 */
    suspend fun processMessageWithAI(
        database: AppDatabase,
        userMessage: String,
        currentState: ConversationState
    ): AssistantResult {

        /*
         * GLOBAL NAVIGATION:
         * "Start another service" is a navigation command, not a service
         * question. Handle it before every other task/state so it cannot
         * accidentally fall through to the finished state, document flow,
         * or Gemini.
         */
        val navigationText = userMessage.lowercase().trim()

        if (
            navigationText == "start another service" ||
            navigationText == "another service" ||
            navigationText == "new service" ||
            navigationText == "start a new service" ||
            navigationText.contains("start another service")
        ) {
            return AssistantResult(
                message = "Sure! What government service do you need help with?",
                state = ConversationState(),
                suggestions = serviceSuggestions(database)
            )
        }

        /*
         * If we are already in the middle of checking
         * documents, keep the existing Yes/No conversation
         * logic. We don't need Gemini for simple answers.
         */
        /*
         * If we are waiting for a simple Yes/No answer, handle that locally.
         *
         * IMPORTANT:
         * A non-Yes/No message is NOT a document answer. It may be a
         * natural-language follow-up such as:
         *   "How do I obtain it?"
         *   "Where can I get it?"
         *   "Can I use another document?"
         *
         * Those messages must continue to Gemini so that the current
         * service/document context can be used.
         */
        if (currentState.waitingForDocumentAnswer) {

            val simpleAnswer =
                normalizeYesNoForAI(userMessage)

            if (simpleAnswer != null) {
                return processMessage(
                    database = database,
                    userMessage = userMessage,
                    currentState = currentState
                )
            }
        }

        /*
         * CHECKLIST COMMANDS MUST BE HANDLED BEFORE GEMINI.
         *
         * A phrase such as "Show my checklist" can be misclassified by an
         * LLM as a document-help request because it contains words related
         * to the current missing document. The checklist command is a
         * deterministic navigation action, so it must always win.
         */
        if (isChecklistRequest(userMessage) && currentState.serviceId != null) {
            val service =
                database.serviceDao()
                    .getServiceById(currentState.serviceId)

            if (service != null) {
                return createFinalResult(
                    database = database,
                    service = service,
                    state = currentState
                )
            }
        }

        /*
         * ------------------------------------------------
         * PHASE 7: SMART CONTEXTUAL HELP
         * ------------------------------------------------
         *
         * These are safe, deterministic conversation helpers. They use the
         * currently verified service/document context and Room data.
         * Gemini is not allowed to invent government facts.
         */
        val contextualService =
            currentState.serviceId?.let { id ->
                database.serviceDao().getServiceById(id)
            }

        if (contextualService != null) {
            val firstStepResult =
                handleFirstStepRequest(
                    database = database,
                    service = contextualService,
                    userMessage = userMessage,
                    currentState = currentState
                )

            if (firstStepResult != null) {
                return firstStepResult
            }

            val explanationResult =
                handleContextExplanationRequest(
                    database = database,
                    service = contextualService,
                    userMessage = userMessage,
                    currentState = currentState
                )

            if (explanationResult != null) {
                return explanationResult
            }
        }

        /*
         * ------------------------------------------------
         * GUIDED PREPARATION / READINESS FLOW
         * ------------------------------------------------
         *
         * A citizen can ask for step-by-step help in ordinary language,
         * such as:
         *   "Where do I start?"
         *   "Guide me through this service"
         *   "Help me prepare"
         *   "Am I ready to apply?"
         *
         * Resolve the service locally when possible, then reuse the same
         * verified document decision flow instead of sending the citizen
         * to a generic service menu.
         */
        if (isGuidedPreparationRequest(userMessage)) {
            val guidedService =
                findServiceForGuidedRequest(
                    database = database,
                    userMessage = userMessage,
                    currentState = currentState
                )

            if (guidedService != null) {
                return startGuidedPreparation(
                    database = database,
                    service = guidedService,
                    currentState = currentState
                )
            }
        }

        /*
         * ------------------------------------------------
         * PROBLEM SELECTION
         * ------------------------------------------------
         *
         * Problem categories are deterministic and come from the
         * verified Problem table. No Gemini call is needed here.
         */
        if (
            currentState.serviceId != null &&
            currentState.task == ConversationTask.PROBLEM_SELECTION
        ) {
            val service =
                database.serviceDao()
                    .getServiceById(currentState.serviceId)

            if (service != null) {
                val selectedType =
                    normalizeProblemType(userMessage)

                if (selectedType != null) {
                    return resolveProblem(
                        database = database,
                        service = service,
                        problemType = selectedType,
                        currentState = currentState
                    )
                }

                return problemSelectionResult(currentState)
            }
        }

        /*
         * ------------------------------------------------
         * DETERMINISTIC COMMON-PROBLEM DETECTION
         * ------------------------------------------------
         *
         * Explicit problem phrases are handled locally. The database
         * remains the source of truth for the actual solution.
         */
        val deterministicProblem =
            detectKnownProblemRequest(
                database = database,
                userMessage = userMessage,
                currentState = currentState
            )

        if (deterministicProblem != null) {
            return resolveProblem(
                database = database,
                service = deterministicProblem.first,
                problemType = deterministicProblem.second,
                currentState = currentState
            )
        }

        /*
         * ------------------------------------------------
         * DEATH CERTIFICATE CONTEXT
         * ------------------------------------------------
         *
         * S002 has conditional documents. Resolve the death context
         * locally so this flow does not depend on Gemini quota.
         */
        if (
            currentState.serviceId == "S002" &&
            currentState.task == ConversationTask.DEATH_CONTEXT
        ) {
            return handleDeathContext(
                database = database,
                userMessage = userMessage,
                currentState = currentState
            )
        }

        /*
         * ------------------------------------------------
         * SMART PROBLEM CONTEXT FOLLOW-UP
         * ------------------------------------------------
         *
         * Once a problem has been identified, citizens often continue with
         * short messages such as:
         *   "What should I do now?"
         *   "What next?"
         *   "How can I fix it?"
         *   "What can I do about this?"
         *
         * These messages do not repeat the problem or service. Reuse the
         * verified problem stored in ConversationState instead of asking
         * Gemini to rediscover the context.
         */
        val contextualProblemFollowUp =
            detectProblemContextFollowUp(userMessage)

        if (
            contextualProblemFollowUp &&
            currentState.serviceId != null &&
            currentState.problemType != null
        ) {
            val service =
                database.serviceDao()
                    .getServiceById(currentState.serviceId)

            if (service != null) {
                return resolveProblem(
                    database = database,
                    service = service,
                    problemType = currentState.problemType,
                    currentState = currentState
                )
            }
        }

        /*
         * ------------------------------------------------
         * SMART DOCUMENT CONTEXT FOLLOW-UP
         * ------------------------------------------------
         *
         * If the citizen is already discussing a document, allow short
         * follow-ups such as "how can I get it?", "where can I get it?",
         * or "can I use something else?" without requiring the document
         * name again or another Gemini call.
         */
        val contextualDocumentAction =
            detectDocumentContextAction(userMessage)

        if (
            contextualDocumentAction != null &&
            currentState.serviceId != null &&
            currentState.documentId != null
        ) {
            val service =
                database.serviceDao()
                    .getServiceById(currentState.serviceId)

            val document =
                database.documentDao()
                    .getAllDocuments()
                    .firstOrNull { it.documentId == currentState.documentId }

            if (service != null && document != null) {
                val localIntent =
                    com.example.aiserviceguide.ai.GeminiIntentResult(
                        service = service.serviceName,
                        intent = contextualDocumentAction,
                        document = document.documentName,
                        problem = "",
                        desiredAction = when (contextualDocumentAction) {
                            "WHERE_TO_OBTAIN_DOCUMENT" -> "WHERE_TO_OBTAIN"
                            "HOW_TO_OBTAIN_DOCUMENT" -> "HOW_TO_OBTAIN"
                            "DOCUMENT_ALTERNATIVE" -> "FIND_ALTERNATIVE"
                            else -> "NONE"
                        },
                        confidence = 1.0
                    )

                return processAIDocumentRequest(
                    database = database,
                    service = service,
                    userMessage = userMessage,
                    intentResult = localIntent,
                    currentState = currentState
                )
            }
        }

        /*
         * ------------------------------------------------
         * SERVICE-LEVEL ACTION MENU
         * ------------------------------------------------
         */
        if (
            currentState.serviceId != null &&
            currentState.task == ConversationTask.SERVICE_ACTIONS
        ) {
            val service =
                database.serviceDao()
                    .getServiceById(currentState.serviceId)

            if (service != null) {
                val actionResult =
                    handleServiceAction(
                        database = database,
                        service = service,
                        userMessage = userMessage,
                        currentState = currentState
                    )

                if (actionResult != null) {
                    return actionResult
                }
            }
        }

        /*
         * ------------------------------------------------
         * DETERMINISTIC MISSING-DOCUMENT DETECTION
         * ------------------------------------------------
         *
         * Gemini is useful for natural-language understanding, but a citizen's
         * explicit statement such as:
         *
         *   "I want an income certificate but I don't have address proof"
         *
         * must not depend on a Gemini call succeeding. In particular, the app
         * may be using a quota-limited Gemini key.
         *
         * We therefore first detect a known service + known document + missing
         * language locally. The database then decides whether that document is
         * REQUIRED / OPTIONAL / NOT_REQUIRED / VERIFY.
         */
        val deterministicMissing =
            detectKnownMissingDocumentRequest(
                database = database,
                userMessage = userMessage,
                currentState = currentState
            )

        if (deterministicMissing != null) {
            return processAIDocumentRequest(
                database = database,
                service = deterministicMissing.first,
                userMessage = userMessage,
                intentResult = deterministicMissing.second,
                currentState = currentState
            )
        }

        /*
         * If a service has already been identified and the assistant is waiting
         * for the citizen to select missing documents, keep this entirely local.
         * Inputs such as "2", "2 and 3", "I have all", or a document name do not
         * need Gemini. This saves API quota and makes the flow deterministic.
         */
        if (
            currentState.serviceId != null &&
            currentState.task == ConversationTask.CHECK_DOCUMENT &&
            !currentState.waitingForDocumentAnswer
        ) {
            return processMessage(
                database = database,
                userMessage = userMessage,
                currentState = currentState
            )
        }

        return try {

            /*
             * Get the actual services from our database.
             */
            val services =
                database.serviceDao()
                    .getAllServices()

            val availableServices =
                services.map {
                    it.serviceName
                }

            /*
             * Ask Gemini to understand the user's message.
             */
            /*
             * Give Gemini the conversation context as well as the new message.
             *
             * This is essential for follow-ups such as:
             * "Where can I get it?"
             * "How do I obtain it?"
             * "Can I use another document?"
             *
             * In those messages the service/document may not be repeated.
             */
            val contextServiceName =
                currentState.serviceId?.let { id ->
                    services.firstOrNull { it.serviceId == id }?.serviceName
                } ?: ""

            val contextDocumentName =
                currentState.documentId?.let { id ->
                    database.documentDao()
                        .getAllDocuments()
                        .firstOrNull { it.documentId == id }
                        ?.documentName
                } ?: ""

            val rawIntentResult =
                GeminiIntentAnalyzer.analyze(
                    userMessage = userMessage,
                    availableServices = availableServices,
                    currentService = contextServiceName,
                    currentDocument = contextDocumentName
                ).getOrThrow()

            /*
             * ------------------------------------------------
             * CONVERSATION CONTEXT
             * ------------------------------------------------
             *
             * Follow-up messages such as:
             *
             *   "Where can I get it?"
             *
             * may not repeat the service or document name.
             * Reuse the verified context stored in ConversationState.
             */
            val contextService =
                currentState.serviceId?.let { id ->
                    services.firstOrNull {
                        it.serviceId == id
                    }
                }

            val allDocuments =
                database.documentDao()
                    .getAllDocuments()

            val contextDocument =
                currentState.documentId?.let { id ->
                    allDocuments.firstOrNull {
                        it.documentId == id
                    }
                }

            val intentResult =
                rawIntentResult.copy(
                    service =
                        rawIntentResult.service
                            .ifBlank {
                                contextService?.serviceName ?: ""
                            },
                    document =
                        rawIntentResult.document
                            .ifBlank {
                                contextDocument?.documentName ?: ""
                            },
                    intent =
                        normalizeContextIntent(
                            rawIntent = rawIntentResult,
                            currentDocumentName =
                                contextDocument?.documentName ?: "",
                            userMessage = userMessage
                        )
                )

            /*
             * For a new conversation, confidence is important.
             * Inside an existing service context, the stored service
             * is authoritative and Gemini only needs to understand
             * the new follow-up action.
             */
            /*
             * Do NOT throw an explicit document problem back into the old
             * rule-based flow just because Gemini returned a modest confidence
             * score.
             *
             * If the citizen explicitly mentions a missing document or asks
             * how/where to obtain it, the deterministic normalization in
             * GeminiIntentAnalyzer has already given us enough information
             * to continue safely.
             */
            if (
                intentResult.service.isBlank() &&
                currentState.serviceId == null
            ) {
                return processMessage(
                    database = database,
                    userMessage = userMessage,
                    currentState = currentState
                )
            }

            /*
             * Find the matching service in our database.
             *
             * Gemini provides the service name, but the database
             * decides whether that service actually exists.
             */
            val service =
                services.firstOrNull {
                    it.serviceName.equals(
                        intentResult.service,
                        ignoreCase = true
                    ) ||
                            it.serviceName.contains(
                                intentResult.service,
                                ignoreCase = true
                            ) ||
                            intentResult.service.contains(
                                it.serviceName,
                                ignoreCase = true
                            )
                }

            /*
             * If Gemini identified a service that isn't
             * present in our database, never invent data.
             */
            if (service == null) {

                return AssistantResult(
                    message = """
                        I understood that you need **${intentResult.service}**.

                        However, I don't currently have verified information for this service in my database.

                        Please choose one of the services I currently support.
                    """.trimIndent(),

                    state = currentState,

                    suggestions =
                        listOf(
                            "Income Certificate",
                            "Caste Certificate",
                            "Passport",
                            "Aadhaar"
                        )
                )
            }

            /*
             * ------------------------------------------------
             * SERVICE-LEVEL ACTIONS
             * ------------------------------------------------
             */
            if (
                intentResult.intent == "APPLICATION_PROCEDURE" ||
                intentResult.intent == "OFFICE_LOCATION" ||
                intentResult.intent == "DEPARTMENT"
            ) {
                val actionResult =
                    handleServiceAction(
                        database = database,
                        service = service,
                        userMessage = userMessage,
                        currentState = currentState
                    )

                if (actionResult != null) {
                    return actionResult
                }
            }

            /*
             * ------------------------------------------------
             * PROBLEM RESOLUTION
             * ------------------------------------------------
             */
            if (
                intentResult.intent == "APPLICATION_PROBLEM" ||
                intentResult.intent == "REJECTION" ||
                intentResult.intent == "STATUS" ||
                intentResult.desiredAction == "SOLVE_PROBLEM"
            ) {
                val problemType =
                    normalizeProblemType(
                        "${intentResult.problem} ${userMessage}"
                    )

                if (problemType != null) {
                    return resolveProblem(
                        database = database,
                        service = service,
                        problemType = problemType,
                        currentState = currentState
                    )
                }

                return problemSelectionResult(
                    currentState = currentState,
                    message = """
                        🛠️ I understand that you are facing a problem with ${service.serviceName}.

                        Please choose the problem type below so I can use the verified solution stored for this service.
                    """.trimIndent()
                )
            }

            /*
             * ------------------------------------------------
             * MISSING DOCUMENT / DOCUMENT HELP
             * ------------------------------------------------
             */
            if (
                intentResult.intent == "MISSING_DOCUMENT" ||
                intentResult.intent == "HOW_TO_OBTAIN_DOCUMENT" ||
                intentResult.intent == "WHERE_TO_OBTAIN_DOCUMENT" ||
                intentResult.intent == "DOCUMENT_ALTERNATIVE"
            ) {

                return processAIDocumentRequest(
                    database = database,
                    service = service,
                    userMessage = userMessage,
                    intentResult = intentResult,
                    currentState = currentState
                )
            }

            /*
             * ------------------------------------------------
             * DOCUMENT REQUIREMENT
             * ------------------------------------------------
             */
            if (
                intentResult.intent ==
                "DOCUMENT_REQUIREMENT"
            ) {

                return processMessage(
                    database = database,
                    userMessage = service.serviceName,
                    currentState = currentState
                )
            }

            /*
             * ------------------------------------------------
             * CHECKLIST
             * ------------------------------------------------
             */
            if (
                intentResult.intent ==
                "CHECKLIST"
            ) {

                return processMessage(
                    database = database,
                    userMessage = "checklist",
                    currentState =
                        currentState.copy(
                            serviceId =
                                service.serviceId,
                            finished = true
                        )
                )
            }

            /*
             * ------------------------------------------------
             * GENERAL SERVICE REQUEST
             * ------------------------------------------------
             *
             * Let the existing database-driven assistant
             * start the normal document conversation.
             */
            return processMessage(
                database = database,
                userMessage = service.serviceName,
                currentState = currentState
            )

        } catch (exception: Exception) {

            /*
             * Gemini/API failure should NOT break the app.
             *
             * Fall back to the existing assistant.
             */
            return processMessage(
                database = database,
                userMessage = userMessage,
                currentState = currentState
            )
        }
    }


    /*
     * Small local Yes/No recognizer used only to decide whether a message
     * should stay inside the simple document-answer flow.
     */
    private fun normalizeYesNoForAI(
        message: String
    ): Boolean? {

        val text =
            message
                .trim()
                .lowercase()
                .replace(".", "")
                .replace("!", "")
                .replace(",", "")

        val yes =
            setOf(
                "yes",
                "yeah",
                "yep",
                "y",
                "yes i have it",
                "i have it",
                "i have",
                "sure",
                "okay",
                "ok"
            )

        val no =
            setOf(
                "no",
                "nope",
                "n",
                "no i don't have it",
                "no i dont have it",
                "i don't have it",
                "i dont have it",
                "i do not have it",
                "don't have it",
                "dont have it"
            )

        return when {
            text in yes -> true
            text in no -> false
            else -> null
        }
    }

    /*
     * ------------------------------------------------
     * AI DOCUMENT REQUEST
     * ------------------------------------------------
     *
     * Uses Gemini to identify the document.
     *
     * Uses Room to retrieve verified information.
     */
    /*
     * When a short follow-up refers to the current document ("it", "this",
     * "that document"), Gemini may return GENERAL_HELP even though the
     * desired action is clear. Use the stored context to make the action
     * deterministic.
     */
    private fun detectProblemContextFollowUp(
        userMessage: String
    ): Boolean {
        val text =
            userMessage
                .lowercase()
                .replace(Regex("\\s+"), " ")
                .trim()

        return listOf(
            "what should i do now",
            "what should i do",
            "what do i do now",
            "what do i do",
            "what next",
            "what should i do next",
            "what can i do",
            "what can i do now",
            "what can i do about this",
            "how can i fix it",
            "how do i fix it",
            "how can i solve it",
            "how do i solve it",
            "how can i fix this",
            "how do i fix this",
            "what is the next step",
            "what's the next step",
            "what is next",
            "what's next",
            "next step",
            "how to fix this",
            "how to solve this"
        ).any { phrase -> text == phrase || text.contains(phrase) }
    }

    private fun detectDocumentContextAction(
        userMessage: String
    ): String? {
        val text =
            userMessage
                .lowercase()
                .replace(Regex("\\s+"), " ")
                .trim()

        val alternative = listOf(
            "can i use another",
            "can i use something else",
            "can i use a different",
            "is there another document",
            "is there another proof",
            "can i use another proof",
            "anything else instead",
            "something else instead",
            "another document instead",
            "another proof instead",
            "can i substitute it",
            "can i substitute this",
            "ಬೇರೆ ದಾಖಲೆ ಇದೆಯಾ",
            "ಬೇರೆ ದಾಖಲೆ ಬಳಸಬಹುದೇ",
            "ಇನ್ನೊಂದು ದಾಖಲೆ ಇದೆಯಾ",
            "ಬೇರೆ ಪುರಾವೆ ಇದೆಯಾ",
            "ಇದಕ್ಕೆ ಬದಲಾಗಿ ಬೇರೆ"
        ).any { text.contains(it) }

        if (alternative) return "DOCUMENT_ALTERNATIVE"

        val where = listOf(
            "where can i get it",
            "where do i get it",
            "where can i obtain it",
            "where do i obtain it",
            "where can i get this",
            "where do i get this",
            "where can i obtain this",
            "where do i obtain this",
            "where can i get that",
            "which office can i get it from",
            "which office can i obtain it from",
            "ಇದು ಎಲ್ಲಿ ಸಿಗುತ್ತದೆ",
            "ಇದನ್ನು ಎಲ್ಲಿ ಪಡೆಯಬಹುದು",
            "ಇದು ಎಲ್ಲಿ ಪಡೆಯಬಹುದು",
            "ಯಾವ ಕಚೇರಿಯಲ್ಲಿ ಸಿಗುತ್ತದೆ",
            "ಯಾವ ಕಚೇರಿಗೆ ಹೋಗಬೇಕು"
        ).any { text.contains(it) }

        if (where) return "WHERE_TO_OBTAIN_DOCUMENT"

        val how = listOf(
            "how can i get it",
            "how do i get it",
            "how can i obtain it",
            "how do i obtain it",
            "how can i get this",
            "how do i get this",
            "how can i obtain this",
            "how do i obtain this",
            "how can i get that",
            "how do i get that",
            "how can i make it",
            "how do i make it",
            "ಇದನ್ನು ಹೇಗೆ ಪಡೆಯುವುದು",
            "ಇದನ್ನು ಹೇಗೆ ಪಡೆಯಬಹುದು",
            "ಇದನ್ನು ಹೇಗೆ ಮಾಡುವುದು",
            "ಹೇಗೆ ಪಡೆಯುವುದು",
            "ಹೇಗೆ ಪಡೆಯಬಹುದು"
        ).any { text.contains(it) }

        if (how) return "HOW_TO_OBTAIN_DOCUMENT"

        return null
    }

    private fun normalizeContextIntent(
        rawIntent: com.example.aiserviceguide.ai.GeminiIntentResult,
        currentDocumentName: String,
        userMessage: String
    ): String {

        if (currentDocumentName.isBlank()) {
            return rawIntent.intent
        }

        val text =
            userMessage.lowercase()

        val asksAlternative =
            listOf(
                "alternative",
                "another document",
                "another proof",
                "other document",
                "other proof",
                "something else instead",
                "substitute",
                "instead",
                "ಬೇರೆ ದಾಖಲೆ",
                "ಬೇರೆ ಪುರಾವೆ",
                "ಇನ್ನೊಂದು ದಾಖಲೆ",
                "ಬದಲಾಗಿ ಬೇರೆ"
            ).any { text.contains(it) }

        if (asksAlternative) {
            return "DOCUMENT_ALTERNATIVE"
        }

        val asksWhere =
            listOf(
                "where can i get",
                "where do i get",
                "where can i obtain",
                "where do i obtain",
                "where to get",
                "where to obtain",
                "which office",
                "which place",
                "ಎಲ್ಲಿ ಸಿಗುತ್ತದೆ",
                "ಎಲ್ಲಿ ಪಡೆಯಬಹುದು",
                "ಯಾವ ಕಚೇರಿ",
                "ಯಾವ ಕಚೇರಿಗೆ"
            ).any { text.contains(it) }

        if (asksWhere) {
            return "WHERE_TO_OBTAIN_DOCUMENT"
        }

        val asksHow =
            listOf(
                "how do i get",
                "how can i get",
                "how to get",
                "how do i obtain",
                "how can i obtain",
                "how to obtain",
                "how can i make",
                "how do i make"
            ).any { text.contains(it) }

        if (asksHow) {
            return "HOW_TO_OBTAIN_DOCUMENT"
        }

        return rawIntent.intent
    }

    private suspend fun detectKnownMissingDocumentRequest(
        database: AppDatabase,
        userMessage: String,
        currentState: ConversationState
    ): Pair<Service, com.example.aiserviceguide.ai.GeminiIntentResult>? {

        val normalized =
            userMessage
                .lowercase()
                .replace(Regex("\\s+"), " ")
                .trim()

        val missingPattern = Regex(
            "\\b(don['’]?t have|do not have|dont have|doesn['’]?t have|does not have|missing|without|no)\\b|ಇಲ್ಲ|ನನ್ನ ಬಳಿ ಇಲ್ಲ|ನನ್ನ ಹತ್ತಿರ ಇಲ್ಲ|ಇಲ್ಲ ನನ್ನ ಬಳಿ"
        )

        if (!missingPattern.containsMatchIn(normalized)) {
            return null
        }

        val services = database.serviceDao().getAllServices()

        val service =
            currentState.serviceId?.let { id ->
                services.firstOrNull { it.serviceId == id }
            } ?: services
                .sortedByDescending { service ->
                    serviceMentionScore(service, normalized)
                }
                .firstOrNull { serviceMentionScore(it, normalized) > 0 }

        if (service == null) {
            return null
        }

        val documents = database.documentDao().getAllDocuments()

        val document =
            documents
                .sortedByDescending { documentMentionScore(it, normalized) }
                .firstOrNull { documentMentionScore(it, normalized) > 0 }

        if (document == null) {
            return null
        }

        val result =
            com.example.aiserviceguide.ai.GeminiIntentResult(
                service = service.serviceName,
                intent = "MISSING_DOCUMENT",
                document = document.documentName,
                problem = "Citizen does not have the mentioned document.",
                desiredAction = "NONE",
                confidence = 1.0
            )

        return service to result
    }


    private fun serviceMentionScore(
        service: Service,
        normalizedMessage: String
    ): Int {
        /*
         * Use the same verified service-keyword matching used by the normal
         * service identification flow.
         *
         * Previously this helper only recognized Income Certificate,
         * Passport and Aadhaar explicitly. That meant a message such as:
         *
         *   "I want to renew my driving licence"
         *
         * correctly revealed the problem type (RENEWAL), but the resolver
         * could not associate the problem with S007 because Driving Licence
         * was missing from this helper. The message then continued to Gemini
         * and Gemini returned only the service menu.
         *
         * calculateScore() already contains the verified keywords for all
         * supported services, so reuse it here. This keeps service matching
         * consistent across the application and does not add any new
         * government facts.
         */
        return calculateScore(
            serviceId = service.serviceId,
            serviceName = service.serviceName,
            question = normalizedMessage
        )
    }


    private fun documentMentionScore(
        document: Document,
        normalizedMessage: String
    ): Int {
        val name = document.documentName.lowercase()

        if (normalizedMessage.contains(name)) return 100

        return when (document.documentId) {
            "D002" -> if (
                normalizedMessage.contains("address proof") ||
                normalizedMessage.contains("proof of address") ||
                normalizedMessage.contains("residence proof") ||
                normalizedMessage.contains("residential proof") ||
                normalizedMessage.contains("address document") ||
                normalizedMessage.contains("residence document") ||
                normalizedMessage.contains("ವಿಳಾಸದ ಪುರಾವೆ") ||
                normalizedMessage.contains("ವಿಳಾಸ ಪುರಾವೆ") ||
                normalizedMessage.contains("ವಿಳಾಸದ ದಾಖಲೆ")
            ) 100 else 0

            "D003" -> if (
                normalizedMessage.contains("identity proof") ||
                normalizedMessage.contains("proof of identity") ||
                normalizedMessage.contains("id proof") ||
                normalizedMessage.contains("ಗುರುತಿನ ಪುರಾವೆ") ||
                normalizedMessage.contains("ಗುರುತಿನ ದಾಖಲೆ")
            ) 100 else 0

            "D006" -> if (
                normalizedMessage.contains("income proof") ||
                normalizedMessage.contains("proof of income") ||
                normalizedMessage.contains("income document") ||
                normalizedMessage.contains("ಆದಾಯದ ಪುರಾವೆ") ||
                normalizedMessage.contains("ಆದಾಯ ಪುರಾವೆ") ||
                normalizedMessage.contains("ಆದಾಯದ ದಾಖಲೆ")
            ) 100 else 0

            else -> 0
        }
    }


    private suspend fun processAIDocumentRequest(
        database: AppDatabase,
        service: Service,
        userMessage: String,
        intentResult: com.example.aiserviceguide.ai.GeminiIntentResult,
        currentState: ConversationState
    ): AssistantResult {

        val documentName = intentResult.document.trim()

        if (documentName.isBlank()) {
            return processMessage(
                database = database,
                userMessage = service.serviceName,
                currentState = currentState
            )
        }

        val allDocuments = database.documentDao().getAllDocuments()

        val document = allDocuments.firstOrNull {
            it.documentName.equals(documentName, ignoreCase = true) ||
                    it.documentName.contains(documentName, ignoreCase = true) ||
                    documentName.contains(it.documentName, ignoreCase = true)
        }

        if (document == null) {
            return AssistantResult(
                message = """
                    I understood that you are asking about $documentName.

                    I do not currently have verified information about this document in my database, so I will not guess whether it is required or tell you an unverified procedure.
                """.trimIndent(),
                state = currentState.copy(
                    serviceId = service.serviceId
                ),
                suggestions = listOf(
                    "Show service requirements",
                    "Show my checklist"
                )
            )
        }

        val allServiceDocuments =
            database.serviceDocumentDao()
                .getDocumentsForService(service.serviceId)

        val contextFilteredServiceDocuments =
            filterServiceDocumentsForDeathContext(
                service = service,
                serviceDocuments = allServiceDocuments,
                currentState = currentState
            )

        val serviceDocument =
            contextFilteredServiceDocuments.firstOrNull {
                it.documentId == document.documentId
            }

        /*
         * For context-sensitive services such as Death Certificate, a document
         * may be valid for the service in general but not applicable to the
         * citizen's selected case. For example, FIR and Postmortem Report are
         * relevant to an unnatural death, but should not be treated as missing
         * requirements for a natural-death case.
         */
        val existsInServiceCatalog =
            allServiceDocuments.any { it.documentId == document.documentId }

        if (service.serviceId == "S002" &&
            existsInServiceCatalog &&
            serviceDocument == null
        ) {
            return AssistantResult(
                message = """
                    📄 ${document.documentName}

                    🟢 This document is not applicable to the Death Certificate case you selected.

                    The app is using your selected death location and death nature to show only the verified documents that apply to your case.

                    I will not mark this document as missing or required when it does not apply to the selected case.
                """.trimIndent(),
                state = currentState.copy(
                    serviceId = service.serviceId,
                    documentId = document.documentId,
                    waitingForDocumentAnswer = false,
                    finished = false,
                    task = ConversationTask.CHECK_DOCUMENT
                ),
                suggestions = listOf(
                    "Show service requirements",
                    "Show my checklist"
                )
            )
        }

        /*
         * A missing relationship row is NOT the same thing as NOT_REQUIRED.
         * It means that the requirement is not listed/known in the verified DB.
         */
        if (serviceDocument == null) {

            val catalog =
                buildServiceDocumentCatalog(
                    serviceDocuments = allServiceDocuments,
                    documentMap = allDocuments.associateBy {
                        it.documentId
                    }
                )

            val fallback = """
                📄 ${document.documentName}

                ⚠️ NOT VERIFIED for ${service.serviceName}.

                This document is not listed in the verified service-document data.

                I will not assume that it is required or not required.
            """.trimIndent()

            return AssistantResult(
                message = fallback,
                state = currentState.copy(
                    serviceId = service.serviceId,
                    documentId = document.documentId,
                    waitingForDocumentAnswer = false,
                    finished = false,
                    task = ConversationTask.MISSING_DOCUMENT
                ),
                suggestions = listOf(
                    "Show service requirements",
                    "Show my checklist"
                )
            )
        }

        val status = normalizeRequirementStatus(serviceDocument.status)

        val catalog =
            buildServiceDocumentCatalog(
                serviceDocuments = allServiceDocuments,
                documentMap = allDocuments.associateBy { it.documentId }
            )

        /*
         * Service-specific source is preferred, then document URL,
         * then the service's main official source.
         */
        val officialUrl =
            serviceDocument.sourceUrl
                .takeIf { it.isNotBlank() }
                ?: document.officialUrl
                    .takeIf { it.isNotBlank() }
                ?: service.officialSource

        val alternative =
            serviceDocument.verifiedAlternative
                .takeIf { it.isNotBlank() }

        val fallback = buildString {
            appendLine("I understand — you do not have ${document.documentName}.")
            appendLine()
            appendLine("🏛️ Service: ${service.serviceName}")
            appendLine()

            when (status) {

                RequirementStatus.REQUIRED -> {
                    appendLine("🔴 ${document.documentName} is marked REQUIRED for this service in the verified database.")
                    appendLine()
                    appendLine("📋 Required documents:")
                    appendLine(catalog)
                    appendLine()
                    appendLine("⚠️ Missing required document:")
                    appendLine(document.documentName)
                    appendLine()

                    if (document.howToObtain.isNotBlank()) {
                        appendLine("📝 How to obtain it:")
                        appendLine(document.howToObtain)
                        appendLine()
                    }

                    if (document.whereToObtain.isNotBlank()) {
                        appendLine("📍 Where to obtain it:")
                        appendLine(document.whereToObtain)
                        appendLine()
                    }

                    appendLine("🔗 Official source:")
                    appendLine(officialUrl)
                    appendLine()
                    appendLine("📞 Official helpline:")
                    appendLine(getOfficialHelpline(database, service.serviceId) ?: "Not currently stored")

                    appendLine()
                    if (alternative != null) {
                        appendLine("🔄 Verified alternative:")
                        appendLine(alternative)
                    } else {
                        appendLine("🔄 Verified alternative: No verified alternative is currently stored for this service.")
                    }

                    appendLine()
                    appendLine("Please resolve this missing required document before treating your preparation as complete.")
                }

                RequirementStatus.NOT_REQUIRED -> {
                    appendLine("🟢 ${document.documentName} is marked NOT REQUIRED for this service.")
                    appendLine()
                    appendLine("You can proceed without this document.")
                    appendLine()
                    appendLine("📋 Service document catalog:")
                    appendLine(catalog)
                    appendLine()
                    appendLine("Please confirm that the documents marked REQUIRED are available.")
                }

                RequirementStatus.OPTIONAL -> {
                    appendLine("🟡 ${document.documentName} is marked OPTIONAL for this service.")
                    appendLine()
                    appendLine("It is not a mandatory document for this service.")
                    appendLine()
                    appendLine("📋 Service document catalog:")
                    appendLine(catalog)
                    appendLine()
                    appendLine("Please make sure the documents marked REQUIRED are available.")
                }

                RequirementStatus.VERIFY -> {
                    appendLine("⚠️ The requirement for ${document.documentName} is NOT YET VERIFIED for this service.")
                    appendLine()
                    appendLine("I will not tell you that it is required or not required until the service requirement is verified.")
                    appendLine()
                    appendLine("📋 Current service document catalog:")
                    appendLine(catalog)
                    appendLine()
                    appendLine("🔗 Official service source:")
                    appendLine(service.officialSource)
                    appendLine()
                    appendLine("📞 Official helpline:")
                    appendLine(getOfficialHelpline(database, service.serviceId) ?: "Not currently stored")
                }

                else -> {
                    appendLine("⚠️ The database contains an unknown requirement status for this document.")
                    appendLine()
                    appendLine("I will not make a requirement decision from an unknown status.")
                    appendLine()
                    appendLine("📋 Service document catalog:")
                    appendLine(catalog)
                }
            }
        }.trim()

        val verifiedFacts = buildString {
            appendLine("Service: ${service.serviceName}")
            appendLine("Department: ${service.department}")
            appendLine("Jurisdiction: ${service.jurisdiction}")
            appendLine("Citizen's missing document: ${document.documentName}")
            appendLine("Verified service-document status: $status")
            appendLine("Service note: ${serviceDocument.notes.ifBlank { "None stored." }}")
            appendLine("How to obtain document: ${document.howToObtain.ifBlank { "No verified acquisition instructions stored." }}")
            appendLine("Where to obtain document: ${document.whereToObtain.ifBlank { "No verified acquisition location stored." }}")
            appendLine("Document official URL: ${document.officialUrl.ifBlank { "None stored." }}")
            appendLine("Service-document source URL: ${serviceDocument.sourceUrl.ifBlank { "None stored." }}")
            appendLine("Service official URL: ${service.officialSource}")
            appendLine("Verified alternative: ${alternative ?: "None stored."}")
            appendLine("Complete service document catalog:")
            appendLine(catalog)
        }

        return AssistantResult(
            message = fallback,
            state = currentState.copy(
                serviceId = service.serviceId,
                documentId = document.documentId,
                waitingForDocumentAnswer = false,
                finished = false,
                missingDocumentIds =
                    if (status == RequirementStatus.REQUIRED) {
                        (currentState.missingDocumentIds + document.documentId).distinct()
                    } else {
                        currentState.missingDocumentIds
                    },
                missingDocumentCursor =
                    if (status == RequirementStatus.REQUIRED) {
                        val existingIndex = currentState.missingDocumentIds.indexOf(document.documentId)
                        if (existingIndex >= 0) existingIndex else currentState.missingDocumentIds.size
                    } else {
                        currentState.missingDocumentCursor
                    },
                task =
                    if (status == RequirementStatus.REQUIRED) {
                        ConversationTask.MISSING_DOCUMENT
                    } else {
                        ConversationTask.CHOOSE_SERVICE
                    }
            ),
            suggestions =
                when (status) {
                    RequirementStatus.REQUIRED -> listOf(
                        "How do I obtain it?",
                        "Where can I get it?",
                        "Show verified alternative",
                        "Show my checklist"
                    )

                    RequirementStatus.NOT_REQUIRED,
                    RequirementStatus.OPTIONAL,
                    RequirementStatus.VERIFY -> listOf(
                        "Show service requirements",
                        "Show my checklist"
                    )

                    else -> listOf(
                        "Show service requirements",
                        "Show my checklist"
                    )
                }
        )
    }

    private fun isChecklistRequest(text: String): Boolean {
        val lower = text.lowercase().trim()

        return lower == "checklist" ||
                lower == "show checklist" ||
                lower == "show my checklist" ||
                lower == "my checklist" ||
                lower == "show the checklist" ||
                lower.contains("show my checklist") ||
                lower.contains("show the checklist") ||
                lower.contains("preparation checklist") ||
                lower.contains("final checklist") ||
                lower.contains("ಚೆಕ್ ಲಿಸ್ಟ್") ||
                lower.contains("ಚೆಕ್ಲಿಸ್ಟ್") ||
                lower.contains("ಚೆಕ್‌ಲಿಸ್ಟ್") ||
                lower.contains("ಚೆಕ್ ಲಿಸ್ಟ್ ತೋರಿಸಿ") ||
                lower.contains("ಚೆಕ್ಲಿಸ್ಟ್ ತೋರಿಸಿ") ||
                lower.contains("ನನ್ನ ಪಟ್ಟಿಯನ್ನು ತೋರಿಸಿ") ||
                lower.contains("ಪಟ್ಟಿ ತೋರಿಸಿ") ||
                lower.contains("ಅಂತಿಮ ಪಟ್ಟಿ") ||
                lower.contains("ನನ್ನ ಚೆಕ್ ಲಿಸ್ಟ್")
    }



    /*
     * ------------------------------------------------
     * SERVICE-LEVEL ACTION MENU
     * ------------------------------------------------
     */

    private suspend fun buildServiceActionMenu(
        database: AppDatabase,
        service: Service,
        currentState: ConversationState
    ): AssistantResult {

        return AssistantResult(
            message = """
                🏛️ ${service.serviceName}

                🏢 ${service.department}

                📍 ${service.jurisdiction}

                What would you like to know?
            """.trimIndent(),

            state =
                currentState.copy(
                    serviceId = service.serviceId,
                    documentId = null,
                    problemType = null,
                    documentIndex = 0,
                    waitingForDocumentAnswer = false,
                    missingDocumentIds = emptyList(),
                    missingDocumentCursor = 0,
                    finished = false,
                    deathLocation = null,
                    deathNature = null,
                    guidedPreparation = false,
                    task = ConversationTask.SERVICE_ACTIONS
                ),

            suggestions =
                listOf(
                    "Guide me",
                    "Required documents",
                    "Where to apply",
                    "I have a problem"
                )
        )
    }


    private suspend fun getOfficialHelpline(
        database: AppDatabase,
        serviceId: String
    ): String? {
        return database.serviceProcedureDao()
            .getProcedureForService(serviceId)
            ?.officialHelpline
            ?.takeIf { it.isNotBlank() }
    }

    private suspend fun handleFirstStepRequest(
        database: AppDatabase,
        service: Service,
        userMessage: String,
        currentState: ConversationState
    ): AssistantResult? {
        val lower = userMessage.lowercase().trim()

        val asksFirstStep =
            lower == "what do i do first" ||
                    lower == "what should i do first" ||
                    lower == "where do i start" ||
                    lower == "where should i start" ||
                    lower == "what is the first step" ||
                    lower == "what's the first step" ||
                    lower.contains("first step") ||
                    lower.contains("do first") ||
                    lower.contains("start with")

        if (!asksFirstStep) return null

        val procedure =
            database.serviceProcedureDao()
                .getProcedureForService(service.serviceId)

        if (procedure == null || procedure.applicationProcedure.isBlank()) {
            return AssistantResult(
                message = """
                    🧭 Where to start

                    🏛️ ${service.serviceName}

                    I do not currently have a verified first application step stored for this service.

                    Please use the official service source for the latest instructions:
                    ${service.officialSource}
                """.trimIndent(),
                state = currentState.copy(finished = false),
                suggestions = listOf(
                    "Required documents",
                    "Guide me step by step",
                    "Official website"
                )
            )
        }

        val firstStep =
            procedure.applicationProcedure
                .lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotBlank() }
                ?: procedure.applicationProcedure.trim()

        return AssistantResult(
            message = """
                🧭 Your first step

                🏛️ ${service.serviceName}

                Start with:
                1. $firstStep

                After that, I can help you check the verified documents and continue step by step.
            """.trimIndent(),
            state = currentState.copy(finished = false),
            suggestions = listOf(
                "Required documents",
                "Guide me step by step",
                "How to apply",
                "Where to apply"
            )
        )
    }

    private suspend fun handleContextExplanationRequest(
        database: AppDatabase,
        service: Service,
        userMessage: String,
        currentState: ConversationState
    ): AssistantResult? {
        val lower = userMessage.lowercase().trim()

        val asksExplanation =
            lower.startsWith("what is ") ||
                    lower.startsWith("what does ") ||
                    lower.startsWith("what do you mean by ") ||
                    lower.startsWith("explain ") ||
                    lower.contains("what does this document mean") ||
                    lower.contains("why do i need this document") ||
                    lower.contains("why is this document needed")

        if (!asksExplanation) return null

        // Prefer the document explicitly stored in conversation context.
        // If the context is currently inside document checking/missing-document
        // flow but documentId is temporarily empty, recover the document from
        // the current cursor before giving up.
        val allDocuments = database.documentDao().getAllDocuments()
        val documentMap = allDocuments.associateBy { it.documentId }

        val serviceDocuments =
            filterServiceDocumentsForDeathContext(
                service = service,
                serviceDocuments = database.serviceDocumentDao()
                    .getDocumentsForService(service.serviceId),
                currentState = currentState
            )

        val requiredDocuments = serviceDocuments.filter {
            normalizeRequirementStatus(it.status) == RequirementStatus.REQUIRED
        }

        val documentId =
            currentState.documentId
                ?: currentState.missingDocumentIds
                    .getOrNull(currentState.missingDocumentCursor)
                ?: requiredDocuments
                    .getOrNull(currentState.documentIndex)
                    ?.documentId

        val document =
            documentId?.let { id -> documentMap[id] }

        if (document == null) {
            return AssistantResult(
                message = """
                    📄 Document explanation

                    I can explain a document using the verified information stored in the app.
                    However, I do not currently have a specific document in the conversation context.

                    Please tell me which document you mean, for example: **Proof of Address**.
                """.trimIndent(),
                state = currentState.copy(finished = false),
                suggestions = listOf(
                    "Show service requirements",
                    "Show my checklist"
                )
            )
        }

        val serviceDocument =
            database.serviceDocumentDao()
                .getDocumentsForService(service.serviceId)
                .firstOrNull { it.documentId == document.documentId }

        val purpose = document.purpose.ifBlank {
            "No verified purpose is currently stored for this document."
        }

        val serviceNote =
            serviceDocument?.notes?.takeIf { it.isNotBlank() }

        return AssistantResult(
            message = buildString {
                appendLine("📘 Simple explanation")
                appendLine()
                appendLine("🏛️ Service: ${service.serviceName}")
                appendLine()
                appendLine("📄 ${document.documentName}")
                appendLine()
                appendLine("What it means:")
                appendLine(purpose)

                if (serviceNote != null) {
                    appendLine()
                    appendLine("ℹ️ Service note:")
                    appendLine(serviceNote)
                }

                appendLine()
                appendLine("This explanation uses information stored for the verified document record.")
            }.trim(),
            state = currentState.copy(finished = false),
            suggestions = listOf(
                "How do I obtain it?",
                "Where can I get it?",
                "Show my checklist"
            )
        )
    }

    private fun isGuidedPreparationRequest(
        text: String
    ): Boolean {
        val lower = text.lowercase().trim()

        return lower.contains("guide me") ||
                lower.contains("step by step") ||
                lower.contains("help me prepare") ||
                lower.contains("where do i start") ||
                lower.contains("how do i prepare") ||
                lower.contains("check if i am ready") ||
                lower.contains("check if i'm ready") ||
                lower.contains("check my readiness") ||
                lower.contains("am i ready")
    }


    private suspend fun findServiceForGuidedRequest(
        database: AppDatabase,
        userMessage: String,
        currentState: ConversationState
    ): Service? {
        currentState.serviceId?.let { id ->
            return database.serviceDao().getServiceById(id)
        }

        val text = userMessage.lowercase().trim()
        val services = database.serviceDao().getAllServices()

        return services
            .map { service ->
                service to calculateScore(
                    service.serviceId,
                    service.serviceName,
                    text
                )
            }
            .maxByOrNull { it.second }
            ?.takeIf { it.second > 0 }
            ?.first
    }


    private suspend fun startGuidedPreparation(
        database: AppDatabase,
        service: Service,
        currentState: ConversationState
    ): AssistantResult {
        val guidedState = currentState.copy(
            serviceId = service.serviceId,
            guidedPreparation = true,
            problemType = null,
            documentId = null,
            documentIndex = 0,
            waitingForDocumentAnswer = false,
            missingDocumentIds = emptyList(),
            missingDocumentCursor = 0,
            finished = false
        )

        val result = showRequiredDocuments(
            database = database,
            service = service,
            currentState = guidedState
        )

        return result.copy(
            message = """
                🧭 Step-by-step preparation

                I will guide you through the verified preparation for ${service.serviceName}.

                I will check the required documents first, then help with missing documents, application steps, and your final readiness checklist.

                ${result.message}
            """.trimIndent(),
            state = result.state.copy(guidedPreparation = true)
        )
    }


    private suspend fun handleServiceAction(
        database: AppDatabase,
        service: Service,
        userMessage: String,
        currentState: ConversationState
    ): AssistantResult? {

        val lower =
            userMessage
                .lowercase()
                .trim()

        if (
            lower.contains("start another service") ||
            lower == "another service" ||
            lower == "new service"
        ) {
            return AssistantResult(
                message =
                    "Sure! What government service do you need help with?",
                state = ConversationState(),
                suggestions = serviceSuggestions(database)
            )
        }

        /*
         * Step-by-step preparation.
         *
         * This is a deterministic entry point into the verified document
         * decision flow. It does not invent requirements.
         */
        if (
            lower == "guide me step by step" ||
            lower == "guide me" ||
            lower.contains("step by step") ||
            lower.contains("help me prepare") ||
            lower.contains("where do i start") ||
            lower.contains("how do i prepare") ||
            lower.contains("check if i am ready") ||
            lower.contains("check if i'm ready") ||
            lower.contains("check my readiness")
        ) {
            return startGuidedPreparation(
                database = database,
                service = service,
                currentState = currentState
            )
        }

        /*
         * Problem resolver.
         */
        if (
            lower == "i have a problem" ||
            lower.contains("problem with this service") ||
            lower.contains("having a problem") ||
            lower.contains("facing a problem")
        ) {
            return problemSelectionResult(
                currentState = currentState.copy(
                    task = ConversationTask.PROBLEM_SELECTION,
                    finished = false
                )
            )
        }

        /*
         * Required documents.
         */
        if (
            lower.contains("required documents") ||
            lower.contains("required document") ||
            lower == "documents" ||
            lower.contains("what documents") ||
            lower.contains("ಅಗತ್ಯ ದಾಖಲೆಗಳು") ||
            lower.contains("ಅಗತ್ಯ ದಾಖಲೆ") ||
            lower.contains("ಯಾವ ದಾಖಲೆಗಳು") ||
            lower.contains("ಯಾವ ದಾಖಲೆ ಬೇಕು") ||
            lower.contains("ಯಾವ ದಾಖಲೆ ಬೇಕಾಗುತ್ತದೆ") ||
            lower.contains("ದಾಖಲೆಗಳು ಯಾವುವು") ||
            lower.contains("ದಾಖಲೆಗಳೇನು") ||
            lower.contains("ಬೇಕಾದ ದಾಖಲೆಗಳು") ||
            lower.contains("ಬೇಕಾದ ದಾಖಲೆ")
        ) {
            return showRequiredDocuments(
                database = database,
                service = service,
                currentState = currentState
            )
        }

        /*
         * Final checklist.
         */
        if (isChecklistRequest(userMessage)) {
            return createFinalResult(
                database = database,
                service = service,
                state = currentState
            )
        }

        /*
         * How to apply.
         *
         * Read the verified service-level procedure from Room.
         * Never invent steps when no verified procedure is stored.
         */
        if (
            lower.contains("how to apply") ||
            lower.contains("how do i apply") ||
            lower.contains("application procedure") ||
            lower == "apply" ||
            lower.contains("ಹೇಗೆ ಅರ್ಜಿ ಸಲ್ಲಿಸಬೇಕು") ||
            lower.contains("ಹೇಗೆ ಅರ್ಜಿ ಹಾಕಬೇಕು") ||
            lower.contains("ಅರ್ಜಿ ಹೇಗೆ ಸಲ್ಲಿಸಬೇಕು") ||
            lower.contains("ಅರ್ಜಿ ಹೇಗೆ ಹಾಕಬೇಕು") ||
            lower.contains("ಅರ್ಜಿಯನ್ನು ಹೇಗೆ ಸಲ್ಲಿಸಬೇಕು") ||
            lower.contains("ಅರ್ಜಿಯನ್ನು ಹೇಗೆ ಹಾಕಬೇಕು") ||
            lower.contains("ಹೇಗೆ ಅರ್ಜಿ") ||
            lower.contains("ಅರ್ಜಿ ಹೇಗೆ")
        ) {
            val procedure =
                database.serviceProcedureDao()
                    .getProcedureForService(service.serviceId)

            if (procedure == null || procedure.applicationProcedure.isBlank()) {
                return AssistantResult(
                    message = """
                        📝 How to apply

                        🏛️ ${service.serviceName}

                        A verified step-by-step application procedure is not currently stored for this service.

                        Please use the official service source for the latest instructions:

                        🔗 ${service.officialSource}

                        📞 Official helpline: ${getOfficialHelpline(database, service.serviceId) ?: "Not currently stored"}
                    """.trimIndent(),
                    state = currentState.copy(
                        task = ConversationTask.SERVICE_ACTIONS,
                        finished = false
                    ),
                    suggestions = listOf(
                        "Required documents",
                        "Where to apply",
                        "Official website",
                        "Show my checklist"
                    )
                )
            }

            return AssistantResult(
                message = buildString {
                    appendLine("📝 How to apply")
                    appendLine()
                    appendLine("🏛️ ${service.serviceName}")
                    appendLine()
                    appendLine(procedure.applicationProcedure)
                    appendLine()
                    appendLine("📍 Where to apply:")
                    appendLine(procedure.whereToApply)
                    appendLine()
                    appendLine("🔗 Official source:")
                    appendLine(procedure.sourceUrl)
                    appendLine()
                    appendLine("📞 Official helpline:")
                    appendLine(procedure.officialHelpline)
                    appendLine()
                    appendLine("ℹ️ ${procedure.verificationNote}")
                }.trim(),
                state = currentState.copy(
                    task = ConversationTask.SERVICE_ACTIONS,
                    finished = false
                ),
                suggestions = listOf(
                    "Required documents",
                    "Where to apply",
                    "Official website",
                    "Show my checklist"
                )
            )
        }

        /*
         * Where to apply.
         *
         * Read the verified service-level application location from Room.
         * Do not invent a physical office when none is stored.
         */
        if (
            lower.contains("where to apply") ||
            lower.contains("where can i apply") ||
            lower.contains("application location") ||
            lower == "location" ||
            lower.contains("ಎಲ್ಲಿ ಅರ್ಜಿ") ||
            lower.contains("ಎಲ್ಲಿ ಅರ್ಜಿ ಸಲ್ಲಿಸಬೇಕು") ||
            lower.contains("ಎಲ್ಲಿ ಅರ್ಜಿ ಹಾಕಬೇಕು") ||
            lower.contains("ಅರ್ಜಿಯನ್ನು ಎಲ್ಲಿ") ||
            lower.contains("ಯಾವ ಕಚೇರಿಯಲ್ಲಿ ಅರ್ಜಿ") ||
            lower.contains("ಯಾವ ಕಚೇರಿಗೆ ಹೋಗಬೇಕು") ||
            lower.contains("ಎಲ್ಲಿ ಹೋಗಬೇಕು") ||
            lower.contains("ಯಾವ ಕಚೇರಿಗೆ")
        ) {
            val procedure =
                database.serviceProcedureDao()
                    .getProcedureForService(service.serviceId)

            if (procedure == null || procedure.whereToApply.isBlank()) {
                return AssistantResult(
                    message = """
                        📍 Where to apply

                        🏛️ ${service.serviceName}

                        A specific verified application location is not currently stored in the database.

                        Please use the official source for the current application channel and office details:

                        🔗 ${service.officialSource}

                        📞 Official helpline: ${getOfficialHelpline(database, service.serviceId) ?: "Not currently stored"}
                    """.trimIndent(),
                    state = currentState.copy(
                        task = ConversationTask.SERVICE_ACTIONS,
                        finished = false
                    ),
                    suggestions = listOf(
                        "Required documents",
                        "How to apply",
                        "Official website",
                        "Show my checklist"
                    )
                )
            }

            return AssistantResult(
                message = buildString {
                    appendLine("📍 Where to apply")
                    appendLine()
                    appendLine("🏛️ ${service.serviceName}")
                    appendLine()
                    appendLine(procedure.whereToApply)
                    appendLine()
                    appendLine("🔗 Official source:")
                    appendLine(procedure.sourceUrl)
                    appendLine()
                    appendLine("📞 Official helpline:")
                    appendLine(procedure.officialHelpline)
                    appendLine()
                    appendLine("ℹ️ ${procedure.verificationNote}")
                }.trim(),
                state = currentState.copy(
                    task = ConversationTask.SERVICE_ACTIONS,
                    finished = false
                ),
                suggestions = listOf(
                    "Required documents",
                    "How to apply",
                    "Official website",
                    "Show my checklist"
                )
            )
        }

        /*
         * Department / office.
         */
        if (
            lower.contains("department") ||
            lower.contains("office") ||
            lower.contains("ಯಾವ ಇಲಾಖೆ") ||
            lower.contains("ಯಾವ ಕಚೇರಿ") ||
            lower.contains("ಕಚೇರಿ ಯಾವುದು")
        ) {
            return AssistantResult(
                message = """
                    🏢 Department / Office

                    ${service.department}

                    📍 Jurisdiction:
                    ${service.jurisdiction}

                    I will not invent a specific counter or office location when it is not stored in the verified database.
                """.trimIndent(),
                state = currentState.copy(
                    task = ConversationTask.SERVICE_ACTIONS,
                    finished = false
                ),
                suggestions = listOf(
                    "Where to apply",
                    "How to apply",
                    "Required documents",
                    "Official website"
                )
            )
        }

        /*
         * Official helpline.
         *
         * Read the verified helpline from the same service procedure record
         * used by the application guidance.
         */
        if (
            lower.contains("official helpline") ||
            lower.contains("helpline number") ||
            lower == "helpline" ||
            lower == "help line" ||
            lower.contains("help line")
        ) {
            val procedure =
                database.serviceProcedureDao()
                    .getProcedureForService(service.serviceId)

            val helpline = procedure?.officialHelpline?.takeIf { it.isNotBlank() }

            return AssistantResult(
                message = if (helpline != null) {
                    """
                        📞 Official Helpline Number

                        🏛️ ${service.serviceName}

                        $helpline

                        Use this official helpline for assistance related to this service.
                    """.trimIndent()
                } else {
                    """
                        📞 Official Helpline Number

                        🏛️ ${service.serviceName}

                        A verified helpline number is not currently stored for this service.

                        🔗 Official service source:
                        ${service.officialSource}
                    """.trimIndent()
                },
                state = currentState.copy(
                    task = ConversationTask.SERVICE_ACTIONS,
                    finished = false
                ),
                suggestions = listOf(
                    "Required documents",
                    "How to apply",
                    "Where to apply",
                    "Official website"
                )
            )
        }

        /*
         * Official website/source.
         */
        if (
            lower.contains("official website") ||
            lower.contains("official site") ||
            lower.contains("website") ||
            lower.contains("official source") ||
            lower.contains("ಅಧಿಕೃತ ವೆಬ್‌ಸೈಟ್") ||
            lower.contains("ಅಧಿಕೃತ ವೆಬ್ಸೈಟ್") ||
            lower.contains("ಅಧಿಕೃತ ಸೈಟ್") ||
            lower.contains("ಅಧಿಕೃತ ಮೂಲ")
        ) {
            return AssistantResult(
                message = """
                    🔗 Official service source

                    ${service.officialSource}

                    📞 Official helpline: ${getOfficialHelpline(database, service.serviceId) ?: "Not currently stored"}

                    Use this official source for the latest service instructions.
                """.trimIndent(),
                state = currentState.copy(
                    task = ConversationTask.SERVICE_ACTIONS,
                    finished = false
                ),
                suggestions = listOf(
                    "Required documents",
                    "How to apply",
                    "Where to apply",
                    "Show my checklist"
                )
            )
        }

        return null
    }


    private fun problemSelectionResult(
        currentState: ConversationState,
        message: String = """
            🛠️ What problem are you facing?

            Choose the option that best matches your situation.
        """.trimIndent()
    ): AssistantResult {
        return AssistantResult(
            message = message,
            state = currentState.copy(
                task = ConversationTask.PROBLEM_SELECTION,
                finished = false
            ),
            suggestions = listOf(
                "Document lost",
                "Application rejected",
                "Wrong information",
                "Renewal",
                "Duplicate document",
                "Update / correction",
                "Application status"
            )
        )
    }


    private fun normalizeProblemType(
        text: String
    ): String? {
        val lower = text.lowercase().trim()

        return when {
            lower.contains("rejected") ||
                    lower.contains("rejection") ||
                    lower.contains("denied") ||
                    lower.contains("returned") ||
                    lower.contains("sent back") ||
                    lower.contains("send back") ||
                    lower.contains("was sent back") ||
                    lower.contains("not approved") ||
                    lower.contains("not been approved") ||
                    lower.contains("application failed") ||
                    lower.contains("failed application") ->
                "APPLICATION_REJECTED"

            lower.contains("lost") ->
                "DUPLICATE_DOCUMENT"

            lower.contains("renew") ||
                    lower.contains("renewal") ||
                    lower.contains("reissue") ->
                "RENEWAL"

            lower.contains("duplicate") ||
                    lower.contains("another copy") ||
                    lower.contains("replacement copy") ->
                "DUPLICATE_DOCUMENT"

            lower.contains("wrong information") ||
                    lower.contains("incorrect information") ||
                    lower.contains("wrong details") ||
                    lower.contains("incorrect details") ||
                    lower.contains("wrong data") ->
                "WRONG_INFORMATION"

            lower.contains("update") ||
                    lower.contains("correction") ||
                    lower.contains("correct my") ||
                    lower.contains("change my details") ||
                    lower.contains("change details") ->
                "UPDATE_CORRECTION"

            lower.contains("status") ||
                    lower.contains("check my application") ||
                    lower.contains("track my application") ->
                "STATUS"

            else -> null
        }
    }


    private suspend fun detectKnownProblemRequest(
        database: AppDatabase,
        userMessage: String,
        currentState: ConversationState
    ): Pair<Service, String>? {
        val text = userMessage.lowercase().trim()

        val problemType = normalizeProblemType(text)
            ?: return null

        val services = database.serviceDao().getAllServices()

        val service =
            currentState.serviceId?.let { id ->
                services.firstOrNull { it.serviceId == id }
            } ?: services
                .sortedByDescending {
                    serviceMentionScore(it, text)
                }
                .firstOrNull {
                    serviceMentionScore(it, text) > 0
                }

        return if (service != null) {
            service to problemType
        } else {
            null
        }
    }


    private suspend fun resolveProblem(
        database: AppDatabase,
        service: Service,
        problemType: String,
        currentState: ConversationState
    ): AssistantResult {
        val problem =
            database.problemDao()
                .getProblem(
                    serviceId = service.serviceId,
                    problemType = problemType
                )

        if (problem == null) {
            return AssistantResult(
                message = """
                    🛠️ Problem Resolver

                    🏛️ Service:
                    ${service.serviceName}

                    ⚠️ I identified the problem, but I do not currently have a verified solution for this exact problem in the database.

                    I will not invent a government procedure.

                    🔗 Official service source:
                    ${service.officialSource}

                    📞 Official helpline: ${getOfficialHelpline(database, service.serviceId) ?: "Not currently stored"}
                """.trimIndent(),
                state = currentState.copy(
                    serviceId = service.serviceId,
                    problemType = problemType,
                    task = ConversationTask.SERVICE_ACTIONS,
                    finished = false
                ),
                suggestions = listOf(
                    "I have a problem",
                    "Required documents",
                    "How to apply",
                    "Official website",
                    "Show my checklist"
                )
            )
        }

        return AssistantResult(
            message = buildString {
                appendLine("🛠️ Problem Resolver")
                appendLine()
                appendLine("🏛️ Service:")
                appendLine(service.serviceName)
                appendLine()
                appendLine("⚠️ Problem:")
                appendLine(problem.title)
                appendLine()
                appendLine("💡 What you should do:")
                appendLine(problem.solution)
                appendLine()
                appendLine("➡️ Next step:")
                appendLine(problem.nextStep)
                appendLine()
                appendLine("🔗 Official source:")
                appendLine(problem.sourceUrl)
                appendLine()
                appendLine("ℹ️ ${problem.verificationNote}")
            }.trim(),
            state = currentState.copy(
                serviceId = service.serviceId,
                problemType = problemType,
                task = ConversationTask.SERVICE_ACTIONS,
                finished = false
            ),
            suggestions = listOf(
                "Required documents",
                "How to apply",
                "Where to apply",
                "I have a problem",
                "Show my checklist"
            )
        )
    }


    private fun deathContextLocationQuestion(
        currentState: ConversationState
    ): AssistantResult {
        return AssistantResult(
            message = """
                🕊️ Death Certificate preparation

                Where did the death occur?

                Please choose one:
                🏥 Hospital / medical institution
                🏠 Home
                📍 Other place
            """.trimIndent(),
            state = currentState.copy(
                task = ConversationTask.DEATH_CONTEXT,
                finished = false
            ),
            suggestions = listOf(
                "Hospital / institution",
                "Home",
                "Other place"
            )
        )
    }

    private fun deathContextNatureQuestion(
        currentState: ConversationState
    ): AssistantResult {
        return AssistantResult(
            message = """
                ⚠️ One more question

                Was the death natural or unnatural?

                This helps me show only the documents that apply to this case.
            """.trimIndent(),
            state = currentState.copy(
                task = ConversationTask.DEATH_CONTEXT,
                finished = false
            ),
            suggestions = listOf(
                "Natural death",
                "Unnatural death"
            )
        )
    }

    private suspend fun handleDeathContext(
        database: AppDatabase,
        userMessage: String,
        currentState: ConversationState
    ): AssistantResult {
        val text = userMessage.lowercase().trim()

        var location = currentState.deathLocation
        var nature = currentState.deathNature

        if (location == null) {
            location = when {
                listOf(
                    "hospital", "medical institution", "institution",
                    "clinic", "nursing home", "health centre", "health center"
                ).any { text.contains(it) } -> "INSTITUTION"

                listOf("home", "house", "at home").any { text.contains(it) } -> "HOME"

                listOf("other", "elsewhere", "other place").any { text.contains(it) } -> "OTHER"

                else -> null
            }

            if (location == null) {
                return deathContextLocationQuestion(currentState)
            }
        }

        if (nature == null) {
            nature = when {
                listOf(
                    "unnatural", "accident", "suicide", "homicide",
                    "poisoning", "unnatural death"
                ).any { text.contains(it) } -> "UNNATURAL"

                listOf(
                    "natural", "natural death", "natural causes",
                    "illness", "disease", "old age"
                ).any { text.contains(it) } -> "NATURAL"

                else -> null
            }

            if (nature == null) {
                return deathContextNatureQuestion(
                    currentState.copy(deathLocation = location)
                )
            }
        }

        val resolvedState = currentState.copy(
            deathLocation = location,
            deathNature = nature,
            task = ConversationTask.SERVICE_ACTIONS,
            finished = false,
            waitingForDocumentAnswer = false,
            documentId = null,
            documentIndex = 0,
            missingDocumentIds = emptyList(),
            missingDocumentCursor = 0
        )

        val service = database.serviceDao().getServiceById("S002")
            ?: return AssistantResult(
                message = "I couldn't load the Death Certificate service from the verified database.",
                state = resolvedState,
                suggestions = emptyList()
            )

        return showRequiredDocuments(
            database = database,
            service = service,
            currentState = resolvedState
        )
    }

    private fun filterServiceDocumentsForDeathContext(
        service: Service,
        serviceDocuments: List<ServiceDocument>,
        currentState: ConversationState
    ): List<ServiceDocument> {
        if (service.serviceId != "S002") return serviceDocuments

        val allowed = mutableSetOf(
            "D003", // Proof of Identity
            "D002", // Proof of Address
            "D011"  // Death Details
        )

        if (currentState.deathLocation == "INSTITUTION") {
            allowed.add("D014")
        }

        if (currentState.deathLocation == "HOME") {
            allowed.add("D015")
        }

        if (currentState.deathNature == "UNNATURAL") {
            allowed.add("D012")
            allowed.add("D013")
        }

        return serviceDocuments.filter { it.documentId in allowed }
    }

    private suspend fun showRequiredDocuments(
        database: AppDatabase,
        service: Service,
        currentState: ConversationState
    ): AssistantResult {

        if (service.serviceId == "S002") {
            if (currentState.deathLocation == null) {
                return deathContextLocationQuestion(currentState)
            }

            if (currentState.deathNature == null) {
                return deathContextNatureQuestion(currentState)
            }
        }

        val allServiceDocuments =
            database.serviceDocumentDao()
                .getDocumentsForService(
                    service.serviceId
                )

        val contextFilteredDocuments =
            filterServiceDocumentsForDeathContext(
                service = service,
                serviceDocuments = allServiceDocuments,
                currentState = currentState
            )

        val requiredRelations =
            contextFilteredDocuments.filter {
                normalizeRequirementStatus(
                    it.status
                ) == RequirementStatus.REQUIRED
            }

        val documents =
            database.documentDao()
                .getAllDocuments()

        val documentMap =
            documents.associateBy {
                it.documentId
            }

        if (requiredRelations.isEmpty()) {
            return AssistantResult(
                message = """
                    📋 Required documents

                    No document is currently marked REQUIRED for ${service.serviceName} in the verified database.

                    I will not treat VERIFY items as mandatory.
                """.trimIndent(),
                state = currentState.copy(
                    task = ConversationTask.SERVICE_ACTIONS,
                    finished = false
                ),
                suggestions = listOf(
                    "How to apply",
                    "Where to apply",
                    "Official website",
                    "Show my checklist"
                )
            )
        }

        val orderedRelations =
            orderServiceDocumentsForDisplay(
                requiredRelations,
                documentMap
            )

        val numbered =
            orderedRelations
                .mapIndexed { index, relation ->
                    val name =
                        documentMap[
                            relation.documentId
                        ]?.documentName
                            ?: relation.documentId

                    "${index + 1}. $name"
                }
                .joinToString("\n")

        val choices =
            orderedRelations
                .mapIndexed { index, relation ->
                    val name =
                        documentMap[
                            relation.documentId
                        ]?.documentName
                            ?: relation.documentId

                    "${index + 1}. $name"
                }
                .toMutableList()

        choices.add("I have all")

        return AssistantResult(
            message = """
                📋 Required documents

                🏛️ ${service.serviceName}

                $numbered

                Which document(s) do you NOT have?
            """.trimIndent(),
            state = currentState.copy(
                task = ConversationTask.CHECK_DOCUMENT,
                waitingForDocumentAnswer = false,
                documentId = null,
                documentIndex = 0,
                missingDocumentIds = emptyList(),
                missingDocumentCursor = 0,
                finished = false
            ),
            suggestions = choices
        )
    }


    suspend fun processMessage(
        database: AppDatabase,
        userMessage: String,
        currentState: ConversationState
    ): AssistantResult {

        val question = userMessage.trim()

        if (question.isBlank()) {
            return AssistantResult(
                message = "Please tell me what government service you need help with.",
                state = currentState,
                suggestions = serviceSuggestions(database)
            )
        }

        if (
            currentState.serviceId == "S002" &&
            currentState.task == ConversationTask.DEATH_CONTEXT
        ) {
            return handleDeathContext(
                database = database,
                userMessage = userMessage,
                currentState = currentState
            )
        }

        /*
         * ------------------------------------------------
         * STEP 1: IDENTIFY SERVICE
         * ------------------------------------------------
         */

        if (currentState.serviceId == null) {

            val services =
                database.serviceDao().getAllServices()

            val normalizedQuestion =
                question.lowercase()

            val bestMatch =
                services
                    .map { service ->
                        service to calculateScore(
                            service.serviceId,
                            service.serviceName,
                            normalizedQuestion
                        )
                    }
                    .maxByOrNull {
                        it.second
                    }

            if (
                bestMatch == null ||
                bestMatch.second == 0
            ) {
                return AssistantResult(
                    message = """
                        I couldn't identify the government service.

                        Please choose a service below, or type/speak your question.
                    """.trimIndent(),
                    state = currentState.copy(
                        task = ConversationTask.CHOOSE_SERVICE
                    ),
                    suggestions = serviceSuggestions(database)
                )
            }

            val service =
                bestMatch.first

            return buildServiceActionMenu(
                database = database,
                service = service,
                currentState = currentState
            )
        }

        /*
         * ------------------------------------------------
         * STEP 2: LOAD CURRENT SERVICE
         * ------------------------------------------------
         */

        val serviceId =
            currentState.serviceId

        if (serviceId == null) {
            return AssistantResult(
                message = "Please tell me which government service you need.",
                state = ConversationState(),
                suggestions = serviceSuggestions(database)
            )
        }

        val service =
            database.serviceDao()
                .getServiceById(serviceId)

        if (service == null) {
            return AssistantResult(
                message = "I couldn't find that service in my database.",
                state = ConversationState(),
                suggestions = serviceSuggestions(database)
            )
        }

        val allServiceDocuments =
            database.serviceDocumentDao()
                .getDocumentsForService(
                    serviceId
                )

        val contextFilteredDocuments =
            filterServiceDocumentsForDeathContext(
                service = service,
                serviceDocuments = allServiceDocuments,
                currentState = currentState
            )

        val serviceDocuments =
            contextFilteredDocuments.filter {
                normalizeRequirementStatus(it.status) == RequirementStatus.REQUIRED
            }

        val documents =
            database.documentDao()
                .getAllDocuments()

        val documentMap =
            documents.associateBy {
                it.documentId
            }

        /*
         * ------------------------------------------------
         * STEP 3: SELECT MISSING DOCUMENTS
         * ------------------------------------------------
         *
         * The citizen has already seen the complete numbered list.
         * Parse simple numbers/names locally. No Gemini call is needed.
         */
        if (
            currentState.task == ConversationTask.CHECK_DOCUMENT &&
            !currentState.waitingForDocumentAnswer
        ) {
            val lower = question.lowercase().trim()

            if (
                lower == "i have all" ||
                lower == "all" ||
                lower == "all documents" ||
                lower.contains("i have all") ||
                lower.contains("have all of them") ||
                lower.contains("i have everything")
            ) {
                return createFinalResult(
                    database,
                    service,
                    currentState.copy(
                        missingDocumentIds = emptyList(),
                        missingDocumentCursor = 0
                    )
                )
            }

            val orderedServiceDocuments =
                orderServiceDocumentsForDisplay(
                    serviceDocuments,
                    documentMap
                )

            val selectedMissingIds = parseMissingDocumentIds(
                question = question,
                requiredDocuments = orderedServiceDocuments,
                documentMap = documentMap
            )

            if (selectedMissingIds.isNotEmpty()) {
                val firstMissingId = selectedMissingIds.first()
                val firstMissingDocument = documentMap[firstMissingId]
                val relation = serviceDocuments.first { it.documentId == firstMissingId }

                return AssistantResult(
                    message = buildSelectedMissingDocumentMessage(
                        document = firstMissingDocument,
                        position = 1,
                        total = selectedMissingIds.size
                    ),
                    state = currentState.copy(
                        documentId = firstMissingId,
                        missingDocumentIds = selectedMissingIds,
                        missingDocumentCursor = 0,
                        waitingForDocumentAnswer = false,
                        finished = false,
                        task = ConversationTask.MISSING_DOCUMENT
                    ),
                    suggestions = missingDocumentSuggestions(firstMissingDocument, relation)
                )
            }

            return AssistantResult(
                message = """
                    Please tell me which required document(s) you do not have.

                    You can use a number such as **2**, or say **I don't have address proof**.
                """.trimIndent(),
                state = currentState.copy(task = ConversationTask.CHECK_DOCUMENT),
                suggestions = listOf(
                    "I have all",
                    "I don't have 2",
                    "I don't have 2 and 3"
                )
            )
        }

        /*
         * ------------------------------------------------
         * STEP 3: MISSING DOCUMENT TASK
         * ------------------------------------------------
         */

        if (
            currentState.task ==
            ConversationTask.MISSING_DOCUMENT
        ) {

            val currentDocument =
                currentState.documentId?.let { id ->
                    serviceDocuments.firstOrNull { it.documentId == id }
                } ?: serviceDocuments.getOrNull(currentState.documentIndex)

            if (currentDocument == null) {
                return createFinalResult(
                    database,
                    service,
                    currentState
                )
            }

            val document =
                documentMap[
                    currentDocument.documentId
                ]

            val lower =
                question.lowercase()

            /*
             * User wants to know HOW to obtain it.
             */

            if (
                lower.contains("how") ||
                lower.contains("obtain") ||
                lower.contains("apply") ||
                lower.contains("get it")
            ) {

                return AssistantResult(
                    message = buildMissingDocumentHowMessage(
                        document,
                        currentDocument
                    ),
                    state = currentState,
                    suggestions = missingDocumentSuggestions(
                        document,
                        currentDocument
                    )
                )
            }

            /*
             * User wants to know WHERE to obtain it.
             */

            if (
                lower.contains("where") ||
                lower.contains("office") ||
                lower.contains("place")
            ) {

                return AssistantResult(
                    message = buildMissingDocumentWhereMessage(
                        document,
                        currentDocument
                    ),
                    state = currentState,
                    suggestions = missingDocumentSuggestions(
                        document,
                        currentDocument
                    )
                )
            }

            /*
             * User asks for verified alternative.
             */

            if (
                lower.contains("alternative") ||
                lower.contains("another document") ||
                lower.contains("instead")
            ) {

                val alternative =
                    currentDocument.verifiedAlternative
                        .takeIf { it.isNotBlank() }

                val message =
                    if (alternative != null) {
                        """
                        🔄 Verified alternative for this service:

                        $alternative

                        This alternative is taken only from the verified service database.
                        """.trimIndent()
                    } else {
                        """
                        🔄 Verified alternative:

                        No verified alternative is currently stored for this service.

                        I will not assume that another document is accepted.
                        """.trimIndent()
                    }

                return AssistantResult(
                    message = message,
                    state = currentState,
                    suggestions = missingDocumentSuggestions(
                        document,
                        currentDocument
                    )
                )
            }

            /*
             * User wants to continue.
             */

            if (
                lower.contains("continue") ||
                lower.contains("next") ||
                lower.contains("proceed") ||
                lower.contains("go on") ||
                lower == "ok" ||
                lower == "okay" ||
                lower == "yes" ||
                lower == "sure"
            ) {

                return continueAfterMissingDocument(
                    service = service,
                    serviceDocuments = serviceDocuments,
                    documentMap = documentMap,
                    currentState = currentState
                )
            }

            /*
             * If the user's answer is not an action,
             * keep the missing-document task active.
             */

            return AssistantResult(
                message = """
                    You don't have this document yet.

                    You can ask me:

                    • How do I obtain it?
                    • Where can I get it?
                    • Is there a verified alternative?

                    Or you can continue to the next document.
                """.trimIndent(),
                state = currentState,
                suggestions = missingDocumentSuggestions(
                    document,
                    currentDocument
                )
            )
        }

        /*
         * ------------------------------------------------
         * STEP 4: CHECK DOCUMENT ANSWER
         * ------------------------------------------------
         */

        if (
            currentState.waitingForDocumentAnswer
        ) {

            val answer =
                normalizeYesNo(question)

            if (answer == null) {
                return AssistantResult(
                    message = """
                        I just need to know whether you have this document.

                        Please choose **Yes** or **No**, or type/speak your answer.
                    """.trimIndent(),
                    state = currentState.copy(
                        task = ConversationTask.CHECK_DOCUMENT
                    ),
                    suggestions = listOf(
                        "Yes, I have it",
                        "No, I don't have it"
                    )
                )
            }

            val currentDocument =
                serviceDocuments.getOrNull(
                    currentState.documentIndex
                )

            if (currentDocument == null) {
                return createFinalResult(
                    database,
                    service,
                    currentState
                )
            }

            val document =
                documentMap[
                    currentDocument.documentId
                ]

            val documentName =
                document?.documentName
                    ?: "this document"

            /*
             * ------------------------------------------------
             * USER HAS DOCUMENT
             * ------------------------------------------------
             */

            if (answer) {

                val nextIndex =
                    currentState.documentIndex + 1

                if (
                    nextIndex >=
                    serviceDocuments.size
                ) {
                    return createFinalResult(
                        database,
                        service,
                        currentState.copy(
                            documentIndex = nextIndex,
                            waitingForDocumentAnswer = false,
                            finished = true,
                            task = ConversationTask.FINISHED
                        )
                    )
                }

                val nextDocument =
                    documentMap[
                        serviceDocuments[
                            nextIndex
                        ].documentId
                    ]

                val nextDocumentName =
                    nextDocument?.documentName
                        ?: "the next document"

                return AssistantResult(
                    message = """
                        Great! 👍

                        I have marked **$documentName** as available.

                        Do you have your **$nextDocumentName**?
                    """.trimIndent(),
                    state = currentState.copy(
                        documentIndex = nextIndex,
                        waitingForDocumentAnswer = true,
                        task = ConversationTask.CHECK_DOCUMENT
                    ),
                    suggestions = listOf(
                        "Yes, I have it",
                        "No, I don't have it"
                    )
                )
            }

            /*
             * ------------------------------------------------
             * USER DOES NOT HAVE DOCUMENT
             * ------------------------------------------------
             *
             * IMPORTANT:
             * We DO NOT immediately move to the next
             * document anymore.
             *
             * This allows contextual suggestions such as:
             *
             * "How do I obtain it?"
             * "Where can I get it?"
             * "Continue"
             */

            val missingMessage =
                buildMissingDocumentMessage(
                    document = document,
                    serviceDocument = currentDocument
                )

            return AssistantResult(
                message = """
                    $missingMessage

                    What would you like to do next?
                """.trimIndent(),
                state = currentState.copy(
                    documentId = currentDocument.documentId,
                    waitingForDocumentAnswer = false,
                    finished = false,
                    missingDocumentIds =
                        (currentState.missingDocumentIds + currentDocument.documentId).distinct(),
                    task = ConversationTask.MISSING_DOCUMENT
                ),
                suggestions = missingDocumentSuggestions(
                    document,
                    currentDocument
                )
            )
        }

        /*
         * ------------------------------------------------
         * STEP 5: CONVERSATION FINISHED
         * ------------------------------------------------
         */

        if (
            currentState.finished ||
            currentState.task ==
            ConversationTask.FINISHED
        ) {

            val lower =
                question.lowercase()

            if (
                lower.contains("checklist") ||
                lower.contains("show") ||
                lower.contains("yes") ||
                lower.contains("sure") ||
                lower.contains("okay") ||
                lower == "ok"
            ) {

                return createFinalResult(
                    database,
                    service,
                    currentState
                )
            }

            if (
                lower.contains("new") ||
                lower.contains("another") ||
                lower.contains("different")
            ) {

                return AssistantResult(
                    message =
                        "Sure! What government service do you need help with?",
                    state = ConversationState(),
                    suggestions = serviceSuggestions(database)
                )
            }

            return AssistantResult(
                message = """
                    I can still help you.

                    You can show your preparation checklist,
                    or start another government service.
                """.trimIndent(),
                state = currentState.copy(
                    finished = true,
                    task = ConversationTask.FINISHED
                ),
                suggestions = listOf(
                    "Show my checklist",
                    "Start another service"
                )
            )
        }

        return AssistantResult(
            message =
                "What would you like to know about this service?",
            state = currentState
        )
    }


    /*
     * ------------------------------------------------
     * CONTINUE AFTER MISSING DOCUMENT
     * ------------------------------------------------
     */

    private fun continueAfterMissingDocument(
        service: Service,
        serviceDocuments: List<ServiceDocument>,
        documentMap: Map<String, Document>,
        currentState: ConversationState
    ): AssistantResult {

        val missingIds = currentState.missingDocumentIds

        if (missingIds.isEmpty()) {
            return AssistantResult(
                message = "No missing documents are recorded. Would you like to see your final checklist?",
                state = currentState.copy(
                    finished = true,
                    task = ConversationTask.FINISHED,
                    waitingForDocumentAnswer = false
                ),
                suggestions = listOf("Show my checklist", "Start another service")
            )
        }

        val nextCursor = currentState.missingDocumentCursor + 1

        if (nextCursor >= missingIds.size) {
            return AssistantResult(
                message = "All the missing documents you selected have been reviewed. Would you like to see your final checklist?",
                state = currentState.copy(
                    missingDocumentCursor = nextCursor,
                    waitingForDocumentAnswer = false,
                    finished = true,
                    task = ConversationTask.FINISHED
                ),
                suggestions = listOf("Show my checklist", "Start another service")
            )
        }

        val nextId = missingIds[nextCursor]
        val nextDocument = documentMap[nextId]
        val relation = serviceDocuments.firstOrNull { it.documentId == nextId }

        if (relation == null) {
            return AssistantResult(
                message = "I could not load the next selected document from the verified service data.",
                state = currentState,
                suggestions = listOf("Show my checklist")
            )
        }

        return AssistantResult(
            message = buildSelectedMissingDocumentMessage(
                document = nextDocument,
                position = nextCursor + 1,
                total = missingIds.size
            ),
            state = currentState.copy(
                documentId = nextId,
                missingDocumentCursor = nextCursor,
                waitingForDocumentAnswer = false,
                finished = false,
                task = ConversationTask.MISSING_DOCUMENT
            ),
            suggestions = missingDocumentSuggestions(nextDocument, relation)
        )
    }


    private fun buildSelectedMissingDocumentMessage(
        document: Document?,
        position: Int,
        total: Int
    ): String {

        val name =
            document?.documentName
                ?: "this document"

        return """
            ⚠️ Missing required document

            $position of $total: $name

            🔴 REQUIRED for this service.

            What would you like to do?
        """.trimIndent()
    }

    /*
     * ------------------------------------------------
     * STABLE DOCUMENT DISPLAY ORDER
     * ------------------------------------------------
     *
     * Room query results should not be treated as insertion order.
     * For the current prototype, use a predictable citizen-friendly
     * order:
     *
     *   1. Proof of Identity
     *   2. Proof of Address
     *   3. Income Evidence
     *
     * Other documents come afterwards.
     *
     * Recommended future upgrade:
     * add ServiceDocument.displayOrder to the database so every
     * government service can define its own verified order.
     */
    private fun orderDocumentsForDisplay(
        documents: List<Document>
    ): List<Document> {
        return documents.sortedBy {
            documentDisplayPriority(it.documentName)
        }
    }

    private fun orderServiceDocumentsForDisplay(
        serviceDocuments: List<ServiceDocument>,
        documentMap: Map<String, Document>
    ): List<ServiceDocument> {
        return serviceDocuments.sortedBy {
            documentDisplayPriority(
                documentMap[it.documentId]?.documentName ?: ""
            )
        }
    }

    private fun documentDisplayPriority(
        documentName: String
    ): Int {
        val name = documentName.lowercase()

        return when {
            name.contains("proof of identity") -> 1
            name.contains("identity") -> 1

            name.contains("proof of address") -> 2
            name.contains("address") -> 2
            name.contains("residence") -> 2

            name.contains("income evidence") -> 3
            name.contains("income") -> 3

            else -> 100
        }
    }


    private fun parseMissingDocumentIds(
        question: String,
        requiredDocuments: List<ServiceDocument>,
        documentMap: Map<String, Document>
    ): List<String> {
        val lower = question.lowercase()
        val result = linkedSetOf<String>()

        Regex("\\b([1-9]\\d*)\\b")
            .findAll(lower)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .forEach { number ->
                requiredDocuments.getOrNull(number - 1)?.let { result.add(it.documentId) }
            }

        requiredDocuments.forEach { relation ->
            val document = documentMap[relation.documentId] ?: return@forEach
            if (documentNameMentioned(document, lower)) result.add(document.documentId)
        }

        return result.toList()
    }

    private fun documentNameMentioned(document: Document, text: String): Boolean {
        val name = document.documentName.lowercase()
        if (text.contains(name)) return true

        return when (document.documentId) {
            "D002" -> listOf("address proof", "proof of address", "residence proof", "residential proof", "address document", "residence document").any { text.contains(it) }
            "D003" -> listOf("identity proof", "proof of identity", "id proof").any { text.contains(it) }
            "D006" -> listOf("income proof", "proof of income", "income document", "income evidence").any { text.contains(it) }
            else -> false
        }
    }


    /*
     * ------------------------------------------------
     * CONTEXT-AWARE SERVICE SUGGESTIONS
     * ------------------------------------------------
     */

    private suspend fun serviceSuggestions(
        database: AppDatabase
    ): List<String> {

        return database.serviceDao()
            .getAllServices()
            .take(4)
            .map {
                it.serviceName
            }
    }


    /*
     * ------------------------------------------------
     * CONTEXT-AWARE MISSING DOCUMENT SUGGESTIONS
     * ------------------------------------------------
     */

    private fun missingDocumentSuggestions(
        document: Document?,
        serviceDocument: ServiceDocument
    ): List<String> {

        val suggestions =
            mutableListOf<String>()

        if (
            !document?.howToObtain
                .isNullOrBlank()
        ) {
            suggestions.add(
                "How do I obtain it?"
            )
        }

        if (
            !document?.whereToObtain
                .isNullOrBlank()
        ) {
            suggestions.add(
                "Where can I get it?"
            )
        }

        if (
            serviceDocument.verifiedAlternative
                .isNotBlank()
        ) {
            suggestions.add(
                "Show verified alternative"
            )
        }

        suggestions.add(
            "Continue"
        )

        return suggestions
    }


    /*
     * ------------------------------------------------
     * MISSING DOCUMENT - HOW
     * ------------------------------------------------
     */

    private fun buildMissingDocumentHowMessage(
        document: Document?,
        serviceDocument: ServiceDocument
    ): String {

        val documentName =
            document?.documentName
                ?: "this document"

        val howToObtain =
            document?.howToObtain
                ?.takeIf {
                    it.isNotBlank()
                }

        return if (howToObtain != null) {

            """
            📋 How to obtain **$documentName**:

            $howToObtain
            """.trimIndent()

        } else {

            """
            📋 How to obtain **$documentName**:

            I don't currently have verified acquisition instructions stored for this document.

            I will not invent a procedure.
            """.trimIndent()
        }
    }


    /*
     * ------------------------------------------------
     * MISSING DOCUMENT - WHERE
     * ------------------------------------------------
     */

    private fun buildMissingDocumentWhereMessage(
        document: Document?,
        serviceDocument: ServiceDocument
    ): String {

        val documentName =
            document?.documentName
                ?: "this document"

        val whereToObtain =
            document?.whereToObtain
                ?.takeIf {
                    it.isNotBlank()
                }

        return if (whereToObtain != null) {

            """
            📍 Where to obtain **$documentName**:

            $whereToObtain
            """.trimIndent()

        } else {

            """
            📍 Where to obtain **$documentName**:

            I don't currently have a verified location or authority stored for this document.

            I will not invent one.
            """.trimIndent()
        }
    }


    /*
     * ------------------------------------------------
     * MISSING DOCUMENT INTELLIGENCE
     * ------------------------------------------------
     */


    private fun buildMissingDocumentMessage(
        document: Document?,
        serviceDocument: ServiceDocument
    ): String {

        val name =
            document?.documentName
                ?: "this document"

        return when (
            normalizeRequirementStatus(
                serviceDocument.status
            )
        ) {

            RequirementStatus.REQUIRED -> """
                ⚠️ Missing required document

                📄 $name

                🔴 REQUIRED for this service.

                What would you like to do?
            """.trimIndent()

            RequirementStatus.NOT_REQUIRED -> """
                🟢 $name is NOT REQUIRED for this service.

                You can proceed without it.
            """.trimIndent()

            RequirementStatus.OPTIONAL -> """
                🟡 $name is OPTIONAL for this service.

                It is not mandatory.
            """.trimIndent()

            RequirementStatus.VERIFY -> """
                ⚠️ $name is not yet verified for this service.

                I will not claim that it is required or not required until the official requirement is verified.
            """.trimIndent()

            else -> """
                ⚠️ The requirement status for $name is unknown.

                I will not make a requirement decision without verified service data.
            """.trimIndent()
        }
    }

    /*
     * ------------------------------------------------
     * FINAL CHECKLIST
     * ------------------------------------------------
     */

    private fun normalizeRequirementStatus(
        status: String?
    ): String {
        return when (status?.trim()?.uppercase()) {
            RequirementStatus.REQUIRED -> RequirementStatus.REQUIRED
            RequirementStatus.OPTIONAL -> RequirementStatus.OPTIONAL
            RequirementStatus.NOT_REQUIRED -> RequirementStatus.NOT_REQUIRED
            RequirementStatus.VERIFY -> RequirementStatus.VERIFY

            // Backward compatibility with the older database.
            "OFFICIAL" -> RequirementStatus.REQUIRED

            else -> "UNKNOWN"
        }
    }


    private fun buildServiceDocumentCatalog(
        serviceDocuments: List<ServiceDocument>,
        documentMap: Map<String, Document>
    ): String {

        if (serviceDocuments.isEmpty()) {
            return "No service-document relationships are stored."
        }

        return serviceDocuments.joinToString(
            separator = "\n"
        ) { relation ->

            val document =
                documentMap[relation.documentId]

            val documentName =
                document?.documentName
                    ?: relation.documentId

            val status =
                normalizeRequirementStatus(relation.status)

            val label =
                when (status) {
                    RequirementStatus.REQUIRED -> "REQUIRED"
                    RequirementStatus.OPTIONAL -> "OPTIONAL"
                    RequirementStatus.NOT_REQUIRED -> "NOT REQUIRED"
                    RequirementStatus.VERIFY -> "VERIFY"
                    else -> "UNKNOWN"
                }

            val note =
                relation.notes
                    .takeIf { it.isNotBlank() }
                    ?.let { " — $it" }
                    ?: ""

            "• $documentName — $label$note"
        }
    }



    private suspend fun createFinalResult(
        database: AppDatabase,
        service: Service,
        state: ConversationState
    ): AssistantResult {

        val allServiceDocuments =
            database.serviceDocumentDao()
                .getDocumentsForService(
                    service.serviceId
                )

        val serviceDocuments =
            filterServiceDocumentsForDeathContext(
                service = service,
                serviceDocuments = allServiceDocuments,
                currentState = state
            )

        val documents =
            database.documentDao()
                .getAllDocuments()

        val documentMap =
            documents.associateBy {
                it.documentId
            }

        val missingIds =
            state.missingDocumentIds.toSet()

        val orderedRelations =
            orderServiceDocumentsForDisplay(
                serviceDocuments,
                documentMap
            )

        val checklistLines =
            orderedRelations.mapIndexed { index, relation ->

                val name =
                    documentMap[
                        relation.documentId
                    ]?.documentName
                        ?: relation.documentId

                val normalizedStatus =
                    normalizeRequirementStatus(
                        relation.status
                    )

                val displayStatus =
                    when (normalizedStatus) {

                        RequirementStatus.REQUIRED -> {
                            if (
                                missingIds.contains(
                                    relation.documentId
                                )
                            ) {
                                "❌ Missing"
                            } else {
                                "✅ Ready"
                            }
                        }

                        RequirementStatus.OPTIONAL ->
                            "ℹ️ Optional"

                        RequirementStatus.NOT_REQUIRED ->
                            "➖ Not required"

                        RequirementStatus.VERIFY ->
                            "⚠️ Verify"

                        else ->
                            "⚠️ Unknown"
                    }

                "${index + 1}. $name — $displayStatus"

            }.joinToString("\n")

        val unresolvedRequired =
            orderedRelations.count { relation ->

                normalizeRequirementStatus(
                    relation.status
                ) == RequirementStatus.REQUIRED &&
                        missingIds.contains(
                            relation.documentId
                        )
            }

        val requiredCount =
            orderedRelations.count {
                normalizeRequirementStatus(it.status) == RequirementStatus.REQUIRED
            }

        val readyRequiredCount =
            (requiredCount - unresolvedRequired).coerceAtLeast(0)

        val documentReadinessPercent =
            if (requiredCount == 0) {
                100
            } else {
                ((readyRequiredCount.toDouble() / requiredCount.toDouble()) * 100.0)
                    .toInt()
                    .coerceIn(0, 100)
            }

        val officialHelpline =
            getOfficialHelpline(
                database = database,
                serviceId = service.serviceId
            )

        val overallReadiness =
            calculateOverallReadinessScore(
                documentReadinessPercent = documentReadinessPercent,
                officialSourceAvailable = service.officialSource.isNotBlank(),
                helplineAvailable = !officialHelpline.isNullOrBlank(),
                jurisdictionAvailable = service.jurisdiction.isNotBlank()
            )

        val readiness =
            if (unresolvedRequired == 0) {
                "🎉 All required documents are currently ready."
            } else {
                "⚠️ $unresolvedRequired required document(s) are still missing."
            }

        val readinessLabel = when {
            overallReadiness >= 90 -> "🟢 Very well prepared"
            overallReadiness >= 70 -> "🟡 Mostly prepared"
            else -> "🔴 More preparation needed"
        }

        val journeyNote =
            if (state.guidedPreparation) {
                "🧭 Guided preparation completed. Review the checklist below before visiting."
            } else {
                ""
            }

        return AssistantResult(

            message = buildString {
                appendLine("📋 Final preparation checklist")
                appendLine()

                if (journeyNote.isNotBlank()) {
                    appendLine(journeyNote)
                    appendLine()
                }

                appendLine("🏛️ ${service.serviceName}")
                appendLine()
                appendLine(readiness)
                appendLine()
                appendLine("📊 Readiness score: $overallReadiness%")
                appendLine(readinessLabel)
                appendLine("Document readiness: $readyRequiredCount/$requiredCount required documents ready")
                appendLine()
                appendLine("📄 Documents")
                appendLine(checklistLines)
                appendLine()
                appendLine("🧳 Before you leave")
                appendLine("☐ Carry original documents")
                appendLine("☐ Carry required photocopies / supporting copies")
                appendLine("☐ Recheck the official source before visiting")
                appendLine("☐ Keep the helpline number saved")
                appendLine()
                appendLine("🏢 Department")
                appendLine(service.department)
                appendLine()
                appendLine("📍 Jurisdiction")
                appendLine(service.jurisdiction)
                appendLine()
                appendLine("🔗 Official source")
                appendLine(service.officialSource)
                appendLine()
                appendLine("📞 Official helpline")
                appendLine(officialHelpline ?: "Not currently stored")
                appendLine()
                appendLine("🔎 Source verification")
                appendLine("Verified against the official source stored in the app dataset.")
                appendLine("Dataset verification date: 22 Sep 2026")
                appendLine("Jurisdiction: ${service.jurisdiction}")
                appendLine()
                appendLine("⚠️ This checklist uses the verified service-document data stored in the app. Requirements can change; use the official source before visiting.")
            }.trim(),

            state =
                state.copy(
                    finished = true,
                    waitingForDocumentAnswer = false,
                    task = ConversationTask.FINISHED
                ),

            suggestions =
                listOf(
                    "Start another service"
                )
        )
    }

    private fun calculateOverallReadinessScore(
        documentReadinessPercent: Int,
        officialSourceAvailable: Boolean,
        helplineAvailable: Boolean,
        jurisdictionAvailable: Boolean
    ): Int {
        val documentWeight = documentReadinessPercent * 0.70
        val sourceWeight = if (officialSourceAvailable) 10.0 else 0.0
        val helplineWeight = if (helplineAvailable) 10.0 else 0.0
        val jurisdictionWeight = if (jurisdictionAvailable) 10.0 else 0.0

        return (documentWeight + sourceWeight + helplineWeight + jurisdictionWeight)
            .toInt()
            .coerceIn(0, 100)
    }

    /*
     * ------------------------------------------------
     * YES / NO UNDERSTANDING
     * ------------------------------------------------
     */

    private fun normalizeYesNo(
        text: String
    ): Boolean? {

        val lower =
            text.lowercase().trim()

        val yesWords = listOf(
            "yes",
            "yeah",
            "yep",
            "yup",
            "i have it",
            "i have",
            "have it",
            "available",
            "okay",
            "ok",
            "sure"
        )

        val noWords = listOf(
            "no",
            "nope",
            "i don't have it",
            "i dont have it",
            "i don't have",
            "i dont have",
            "not available",
            "don't have",
            "dont have",
            "missing"
        )

        if (
            noWords.any {
                lower.contains(it)
            }
        ) {
            return false
        }

        if (
            yesWords.any {
                lower.contains(it)
            }
        ) {
            return true
        }

        return null
    }


    /*
     * ------------------------------------------------
     * SERVICE MATCHING
     * ------------------------------------------------
     */

    private fun calculateScore(
        serviceId: String,
        serviceName: String,
        question: String
    ): Int {

        var score = 0

        val serviceNameLower =
            serviceName.lowercase()

        if (
            question.contains(
                serviceNameLower
            )
        ) {
            score += 10
        }

        val keywords =
            when (serviceId) {

                "S001" -> listOf(
                    "birth",
                    "birth certificate",
                    "birth registration",
                    "newborn",
                    "proof of birth",
                    "ಜನನ ಪ್ರಮಾಣಪತ್ರ",
                    "ಜನನ ಪ್ರಮಾಣ ಪತ್ರ"
                )

                "S002" -> listOf(
                    "death",
                    "death certificate",
                    "death registration",
                    "proof of death",
                    "ಮರಣ ಪ್ರಮಾಣಪತ್ರ",
                    "ಮರಣ ಪ್ರಮಾಣ ಪತ್ರ"
                )

                "S003" -> listOf(
                    "income",
                    "income certificate",
                    "income proof",
                    "earnings proof",
                    "annual income",
                    "ಆದಾಯ ಪ್ರಮಾಣಪತ್ರ",
                    "ಆದಾಯ ಪ್ರಮಾಣ ಪತ್ರ"
                )

                "S004" -> listOf(
                    "residence",
                    "residence certificate",
                    "domicile",
                    "domicile certificate",
                    "resident certificate",
                    "residential proof",
                    "ವಾಸಸ್ಥಳ ಪ್ರಮಾಣಪತ್ರ",
                    "ನಿವಾಸ ಪ್ರಮಾಣಪತ್ರ"
                )

                "S005" -> listOf(
                    "caste",
                    "caste certificate",
                    "community certificate",
                    "community proof",
                    "ಜಾತಿ ಪ್ರಮಾಣಪತ್ರ",
                    "ಜಾತಿ ಪ್ರಮಾಣ ಪತ್ರ"
                )

                "S006" -> listOf(
                    "ration",
                    "ration card",
                    "food card",
                    "pds card",
                    "food supplies",
                    "ಪಡಿತರ ಚೀಟಿ",
                    "ರೇಷನ್ ಕಾರ್ಡ್"
                )

                "S007" -> listOf(
                    "driving",
                    "driving licence",
                    "driving license",
                    "learner licence",
                    "learner license",
                    "driving permit",
                    "dl",
                    "rto",
                    "ಚಾಲನಾ ಪರವಾನಗಿ",
                    "ಡ್ರೈವಿಂಗ್ ಲೈಸೆನ್ಸ್"
                )

                "S008" -> listOf(
                    "vehicle",
                    "vehicle registration",
                    "registration certificate",
                    "vehicle rc",
                    "rc",
                    "rc book",
                    "registration card"
                )

                "S009" -> listOf(
                    "passport",
                    "passport application",
                    "passport renewal",
                    "passport reissue",
                    "travel passport"
                )

                "S010" -> listOf(
                    "aadhaar",
                    "aadhar",
                    "aadhaar card",
                    "uidai",
                    "aadhaar update",
                    "aadhaar enrolment",
                    "aadhaar enrollment",
                    "biometric update",
                    "aadhaar correction"
                )

                else -> emptyList()
            }

        keywords.forEach { keyword ->

            if (
                question.contains(keyword)
            ) {
                score += 3
            }
        }

        return score
    }
}