package com.example.aiserviceguide.data

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

enum class ConversationTask {
    CHOOSE_SERVICE,
    SERVICE_ACTIONS,
    PROBLEM_SELECTION,
    CHECK_DOCUMENT,
    DEATH_CONTEXT,
    MISSING_DOCUMENT,
    FINISHED
}

data class ConversationState(
    val serviceId: String? = null,
    val documentId: String? = null,
    val problemType: String? = null,
    val guidedPreparation: Boolean = false,
    val documentIndex: Int = 0,
    val waitingForDocumentAnswer: Boolean = false,
    val missingDocumentIds: List<String> = emptyList(),
    val missingDocumentCursor: Int = 0,
    val finished: Boolean = false,
    val deathLocation: String? = null,
    val deathNature: String? = null,
    val task: ConversationTask = ConversationTask.CHOOSE_SERVICE
)

data class AssistantResult(
    val message: String,
    val state: ConversationState,
    val suggestions: List<String> = emptyList()
)
