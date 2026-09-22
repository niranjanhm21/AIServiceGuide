package com.example.aiserviceguide

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.aiserviceguide.data.ChatMessage
import com.example.aiserviceguide.data.ConversationState
import com.example.aiserviceguide.data.ConversationTask
import com.example.aiserviceguide.data.DatabaseProvider
import com.example.aiserviceguide.data.DatabaseTest
import com.example.aiserviceguide.data.ServiceAssistant
import com.example.aiserviceguide.data.Service
import com.example.aiserviceguide.ui.theme.AIServiceGuideTheme
import com.example.aiserviceguide.ai.GeminiService
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Test the database once when the activity starts.
         */
        lifecycleScope.launch {

            val result =
                DatabaseTest.testDatabase(this@MainActivity)

            Toast.makeText(
                this@MainActivity,
                result,
                Toast.LENGTH_LONG
            ).show()
        }

        setContent {

            AIServiceGuideTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF7FAFF)
                ) {

                    ChatScreen()
                }
            }
        }
    }
}


/*
 * ------------------------------------------------
 * CONVERSATION NAVIGATION SNAPSHOT
 * ------------------------------------------------
 *
 * Back navigation restores the previous conversation state and suggestions
 * without deleting any messages already shown in the chat.
 */
private data class ConversationNavigationSnapshot(
    val state: ConversationState,
    val suggestions: List<String>
)

/*
 * ------------------------------------------------
 * CHAT SCREEN
 * ------------------------------------------------
 */

@Composable
fun ChatScreen() {

    val context = LocalContext.current

    /*
     * Database instance.
     */
    val database =
        remember(context) {
            DatabaseProvider.getDatabase(context)
        }

    /*
     * Chat messages.
     */
    var messages by remember {

        mutableStateOf(
            listOf(
                ChatMessage(
                    text = """
                        Hello! 👋

                        I am your AI Service Guide.

                        I can help you prepare before visiting a government office.

                        Tell me which government service you need.
                    """.trimIndent(),
                    isUser = false
                )
            )
        )
    }

    /*
     * Current conversation state.
     */
    var conversationState by remember {

        mutableStateOf(
            ConversationState()
        )
    }

    /*
     * Suggested replies.
     */
    var suggestions by remember {

        mutableStateOf(
            listOf(
                "Income Certificate",
                "Caste Certificate",
                "Passport",
                "Aadhaar"
            )
        )
    }

    /*
     * Text entered by user.
     */
    var inputText by remember {

        mutableStateOf("")
    }

    /*
     * Processing state.
     */
    var isProcessing by remember {

        mutableStateOf(false)
    }

    /*
     * Microphone state.
     */
    var isListening by remember {
        mutableStateOf(false)
    }

    var speechToSend by remember {
        mutableStateOf("")
    }

    /*
     * Voice language for the speech recognizer.
     * English remains the default so existing voice behaviour is unchanged.
     * Kannada can be selected when the citizen wants to speak in Kannada.
     */
    var speechLanguage by remember {
        mutableStateOf("en-IN")
    }

    var showSpeechLanguageMenu by remember {
        mutableStateOf(false)
    }

    /*
     * All services for the service picker.
     * Loaded directly from the verified Room database.
     */
    var availableServices by remember {
        mutableStateOf<List<Service>>(emptyList())
    }

    var showServicePicker by remember {
        mutableStateOf(false)
    }

    /*
     * Phase 10: simple service discovery on the home screen.
     * This is local UI filtering over the verified Room service list.
     */
    var serviceSearchText by remember {
        mutableStateOf("")
    }

    var showHowItWorks by remember {
        mutableStateOf(false)
    }

    /*
     * Phase 8: expandable contextual help menu.
     * The normal quick options remain small, while More help exposes
     * the less-visible capabilities of the assistant.
     */
    var showMoreHelp by remember {
        mutableStateOf(false)
    }

    /*
     * Phase 9: conversation navigation.
     *
     * We keep previous conversation states separately from the visible
     * messages. This lets the citizen go back without losing the chat.
     */
    var navigationHistory by remember {
        mutableStateOf<List<ConversationNavigationSnapshot>>(emptyList())
    }

    var showMainMenu by remember {
        mutableStateOf(false)
    }


    LaunchedEffect(database) {
        availableServices = database.serviceDao().getAllServices()
    }

    /*
     * Coroutine scope.
     */
    val scope = rememberCoroutineScope()

    /*
     * Chat list state.
     */
    val listState = rememberLazyListState()


    /*
     * ------------------------------------------------
     * SPEECH RECOGNIZER
     * ------------------------------------------------
     */

    val speechRecognizer =
        remember(context) {

            if (
                SpeechRecognizer.isRecognitionAvailable(
                    context
                )
            ) {

                SpeechRecognizer.createSpeechRecognizer(
                    context
                )

            } else {

                null
            }
        }


    /*
     * ------------------------------------------------
     * SPEECH RECOGNITION LISTENER
     * ------------------------------------------------
     */

    DisposableEffect(speechRecognizer) {

        speechRecognizer?.setRecognitionListener(

            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {

                    isListening = true
                }


                override fun onBeginningOfSpeech() {

                    isListening = true
                }


                override fun onRmsChanged(
                    rmsdB: Float
                ) {
                }


                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {
                }


                override fun onEndOfSpeech() {

                    isListening = false
                }


                override fun onError(
                    error: Int
                ) {

                    isListening = false

                    val errorMessage =
                        when (error) {

                            SpeechRecognizer.ERROR_AUDIO ->
                                "Microphone audio error."

                            SpeechRecognizer.ERROR_CLIENT ->
                                "Speech recognition client error."

                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                "Microphone permission is not available."

                            SpeechRecognizer.ERROR_NETWORK ->
                                "Network error during speech recognition."

                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                "Speech recognition network timeout."

                            SpeechRecognizer.ERROR_NO_MATCH ->
                                "I could not understand what you said. Please try again."

                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                                "Speech recognition is busy. Please try again."

                            SpeechRecognizer.ERROR_SERVER ->
                                "Speech recognition server error."

                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                "I did not hear anything. Please speak again."

                            else ->
                                "Speech recognition error: $error"
                        }


                    Toast.makeText(
                        context,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }


                override fun onResults(results: Bundle?) {
                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    if (!matches.isNullOrEmpty()) {
                        val recognizedText = matches[0].trim()

                        if (recognizedText.isNotBlank()) {
                            inputText = recognizedText

                            // Tell Compose that speech is ready to send
                            speechToSend = recognizedText
                        }
                    }

                    isListening = false
                }


                override fun onPartialResults(
                    partialResults: Bundle?
                ) {
                }


                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }
        )


        onDispose {

            speechRecognizer?.destroy()
        }
    }


    /*
     * ------------------------------------------------
     * MICROPHONE PERMISSION
     * ------------------------------------------------
     */

    val microphonePermissionLauncher =
        rememberLauncherForActivityResult(

            ActivityResultContracts.RequestPermission()

        ) { granted ->

            if (granted) {

                startSpeechRecognition(
                    speechRecognizer,
                    context,
                    speechLanguage
                )

            } else {

                Toast.makeText(
                    context,
                    "Microphone permission is required.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


    /*
     * ------------------------------------------------
     * PHASE 9: NAVIGATION HELPERS
     * ------------------------------------------------
     */

    fun goBack() {
        showMoreHelp = false
        showMainMenu = false
        showServicePicker = false

        val previous = navigationHistory.lastOrNull()

        if (previous != null) {
            navigationHistory = navigationHistory.dropLast(1)
            conversationState = previous.state
            suggestions = previous.suggestions
            Toast.makeText(
                context,
                "Returned to the previous step.",
                Toast.LENGTH_SHORT
            ).show()
        } else if (conversationState.serviceId != null) {
            /*
             * No stored step remains, so return to the service menu.
             * Messages are intentionally kept visible.
             */
            conversationState = ConversationState()
            suggestions = availableServices.map { it.serviceName }
            showServicePicker = false

            Toast.makeText(
                context,
                "Returned to the service menu.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "You are already at the beginning.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun openServiceMenu() {
        showMainMenu = false
        showMoreHelp = false
        showServicePicker = true
        serviceSearchText = ""
        showHowItWorks = false
        conversationState = ConversationState()
        suggestions = availableServices.map { it.serviceName }
    }

    fun startAnotherService() {
        showMainMenu = false
        showMoreHelp = false
        showServicePicker = true
        serviceSearchText = ""
        showHowItWorks = false
        conversationState = ConversationState()
        suggestions = availableServices.map { it.serviceName }
    }

    /*
     * ------------------------------------------------
     * SEND MESSAGE FUNCTION
     * ------------------------------------------------
     */

    fun sendMessage(
        message: String
    ) {

        val cleanMessage =
            message.trim()


        if (
            cleanMessage.isBlank()
        ) {

            return
        }

        /*
         * FINAL PHASE A:
         * Natural navigation commands work from the text box too.
         * These are UI navigation commands, so they are handled locally
         * and never sent to Gemini.
         */
        val navigationCommand = cleanMessage.lowercase().trim()

        if (
            navigationCommand == "back" ||
            navigationCommand == "go back" ||
            navigationCommand == "previous" ||
            navigationCommand == "previous step" ||
            navigationCommand == "go to previous"
        ) {
            goBack()
            return
        }

        if (
            navigationCommand == "home" ||
            navigationCommand == "go home" ||
            navigationCommand == "service menu" ||
            navigationCommand == "main menu" ||
            navigationCommand == "menu"
        ) {
            openServiceMenu()
            return
        }

        if (
            navigationCommand == "start over" ||
            navigationCommand == "restart" ||
            navigationCommand == "start again"
        ) {
            startAnotherService()
            return
        }

        /*
         * Remember where the citizen was before this action.
         * We cap the stack so a long conversation cannot grow without bound.
         */
        navigationHistory =
            (navigationHistory +
                    ConversationNavigationSnapshot(
                        state = conversationState,
                        suggestions = suggestions
                    )).takeLast(30)

        /*
         * Add user message.
         */
        messages =
            messages +
                    ChatMessage(
                        text = cleanMessage,
                        isUser = true
                    )


        /*
         * Clear input.
         */
        inputText = ""


        /*
         * Remove old suggestions while processing.
         */
        suggestions =
            emptyList()

        // Close feature discovery while a request is being processed.
        showMoreHelp = false


        /*
         * Show loading state.
         */
        isProcessing = true


        /*
         * Process using ServiceAssistant.
         */
        scope.launch {

            try {

                val result =
                    ServiceAssistant.processMessageWithAI(
                        database = database,
                        userMessage = cleanMessage,
                        currentState = conversationState
                    )


                /*
                 * Add AI response.
                 */
                messages =
                    messages +
                            ChatMessage(
                                text = result.message,
                                isUser = false
                            )


                /*
                 * Update conversation state.
                 */
                conversationState =
                    result.state


                /*
                 * Update suggestions.
                 */
                suggestions =
                    result.suggestions

            } catch (exception: Exception) {

                messages =
                    messages +
                            ChatMessage(
                                text = """
                                    Sorry, something went wrong while processing your request.

                                    Please try again.
                                """.trimIndent(),
                                isUser = false
                            )

                Toast.makeText(
                    context,
                    exception.message
                        ?: "Unknown error",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                isProcessing = false
            }
        }
    }
    LaunchedEffect(speechToSend) {
        if (speechToSend.isNotBlank() && !isProcessing) {
            val message = speechToSend

            speechToSend = ""

            sendMessage(message)
        }
    }



    /*
     * ------------------------------------------------
     * AUTO-SCROLL CHAT
     * ------------------------------------------------
     */

    LaunchedEffect(
        messages.size
    ) {

        if (
            messages.isNotEmpty()
        ) {

            listState.animateScrollToItem(
                messages.lastIndex
            )
        }
    }


    /*
     * ------------------------------------------------
     * MAIN LAYOUT
     * ------------------------------------------------
     */

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEAF4FF),
                            Color(0xFFF7FAFF),
                            Color(0xFFFDFEFF)
                        )
                    )
                )
                .imePadding()
    ) {


        /*
         * ------------------------------------------------
         * HEADER
         * ------------------------------------------------
         */

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0D47A1),
                                Color(0xFF1976D2),
                                Color(0xFF42A5F5)
                            )
                        )
                    )
                    .padding(
                        start = 10.dp,
                        end = 10.dp,
                        top = 8.dp,
                        bottom = 10.dp
                    )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                /*
                 * Back button.
                 */
                Text(
                    text = "‹",
                    fontSize = 36.sp,
                    color = Color.White,
                    modifier = Modifier
                        .size(46.dp)
                        .clickable(
                            enabled =
                                navigationHistory.isNotEmpty() ||
                                        conversationState.serviceId != null
                        ) {
                            goBack()
                        }
                        .padding(bottom = 4.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.width(2.dp))

                Box(
                    modifier =
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Color.White.copy(alpha = 0.18f)
                            ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏛️",
                        fontSize = 21.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "AI Service Guide",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text =
                            if (conversationState.serviceId != null) {
                                "Service preparation assistant"
                            } else {
                                "Government service preparation assistant"
                            },
                        fontSize = 12.sp,
                        color = Color(0xFFE3F2FD),
                        maxLines = 1
                    )
                }

                /*
                 * Main menu button.
                 */
                Text(
                    text = "☰",
                    fontSize = 25.sp,
                    color = Color.White,
                    modifier = Modifier
                        .size(46.dp)
                        .clickable {
                            showMainMenu = !showMainMenu
                            showMoreHelp = false
                            showServicePicker = false
                        }
                        .padding(top = 6.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        /*
         * ------------------------------------------------
         * PHASE 9: MAIN NAVIGATION MENU
         * ------------------------------------------------
         */
        if (showMainMenu) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 6.dp,
                        bottom = 4.dp
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 5.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(
                        text = "☰ Menu",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(
                            start = 8.dp,
                            bottom = 6.dp
                        )
                    )

                    NavigationMenuItem(
                        icon = "🏠",
                        title = "Service menu",
                        subtitle = "Choose a government service",
                        onClick = {
                            openServiceMenu()
                        }
                    )

                    NavigationMenuItem(
                        icon = "➕",
                        title = "Start another service",
                        subtitle = "Begin a new service journey",
                        onClick = {
                            startAnotherService()
                        }
                    )

                    if (conversationState.serviceId != null) {
                        NavigationMenuItem(
                            icon = "📍",
                            title = "Current service",
                            subtitle = "Continue with the selected service",
                            onClick = {
                                showMainMenu = false
                            }
                        )
                    }

                    if (navigationHistory.isNotEmpty()) {
                        NavigationMenuItem(
                            icon = "←",
                            title = "Go back",
                            subtitle = "Return to the previous step",
                            onClick = {
                                goBack()
                            }
                        )
                    }
                }
            }
        }


        /*
         * ------------------------------------------------
         * PHASE 9: CURRENT JOURNEY INDICATOR
         * ------------------------------------------------
         */
        if (conversationState.serviceId != null) {
            val journeyLabel =
                when (conversationState.task) {
                    ConversationTask.SERVICE_ACTIONS -> "Service menu"
                    ConversationTask.PROBLEM_SELECTION -> "Problem & updates"
                    ConversationTask.CHECK_DOCUMENT -> "Document check"
                    ConversationTask.DEATH_CONTEXT -> "Service details"
                    ConversationTask.MISSING_DOCUMENT -> "Missing document"
                    ConversationTask.FINISHED -> "Preparation complete"
                    ConversationTask.CHOOSE_SERVICE -> "Choose service"
                }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 6.dp,
                        bottom = 2.dp
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEAF4FF)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📍",
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = "Current service",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF486581)
                    )

                    Text(
                        text = "  ›  ",
                        fontSize = 11.sp,
                        color = Color(0xFF78909C)
                    )

                    Text(
                        text = journeyLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1565C0),
                        maxLines = 1
                    )
                }
            }
        }

        /*
         * ------------------------------------------------
         * CHAT MESSAGES
         * ------------------------------------------------
         */

        LazyColumn(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp
                    ),
            state = listState,
            verticalArrangement =
                Arrangement.spacedBy(10.dp),
            contentPadding =
                PaddingValues(
                    top = 16.dp,
                    bottom = 12.dp
                )
        ) {

            items(
                items = messages
            ) { message ->

                ChatBubble(
                    message = message
                )
            }


            /*
             * Loading indicator.
             */
            if (isProcessing) {

                item {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.Start
                    ) {

                        Card(
                            shape =
                                RoundedCornerShape(
                                    18.dp
                                ),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        Color.White
                                )
                        ) {

                            Row(
                                modifier =
                                    Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 12.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(
                                            18.dp
                                        ),
                                    strokeWidth = 2.dp
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(10.dp)
                                )

                                Text(
                                    text =
                                        "Thinking...",
                                    color =
                                        Color(0xFF486581),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }


        /*
         * ------------------------------------------------
         * PHASE 10: HOME / SERVICE DISCOVERY
         * ------------------------------------------------
         *
         * The home screen now answers three questions immediately:
         * 1. What can this app do?
         * 2. How does it work?
         * 3. How do I find my service?
         *
         * No new database data is introduced. Search is performed only
         * against the verified services already loaded from Room.
         */
        if (
            !isProcessing &&
            conversationState.serviceId == null &&
            availableServices.isNotEmpty()
        ) {
            val filteredServices =
                availableServices.filter { service ->
                    val query = serviceSearchText.trim()

                    query.isBlank() ||
                            service.serviceName.contains(
                                query,
                                ignoreCase = true
                            ) ||
                            service.description.contains(
                                query,
                                ignoreCase = true
                            ) ||
                            service.department.contains(
                                query,
                                ignoreCase = true
                            )
                }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 6.dp,
                        bottom = 4.dp
                    ),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "👋 What do you need help with?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF17324D)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text =
                            "Choose a government service and I'll help you understand what to prepare before you visit.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF607D8B)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = serviceSearchText,
                        onValueChange = {
                            serviceSearchText = it
                            showServicePicker = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Search for a service...",
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Text(text = "🔎")
                        },
                        trailingIcon = {
                            if (serviceSearchText.isNotBlank()) {
                                Text(
                                    text = "✕",
                                    modifier = Modifier
                                        .clickable {
                                            serviceSearchText = ""
                                        }
                                        .padding(8.dp),
                                    color = Color(0xFF607D8B),
                                    fontSize = 14.sp
                                )
                            }
                        },
                        shape = RoundedCornerShape(13.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            showHowItWorks = !showHowItWorks
                            showServicePicker = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFF90CAF9)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFF7FBFF),
                            contentColor = Color(0xFF1565C0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 How does it work?",
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = if (showHowItWorks) "▲" else "▼",
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (showHowItWorks) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HomeHowItWorksRow(
                                number = "1",
                                title = "Choose your service",
                                description = "Tell me what government service you need."
                            )

                            HomeHowItWorksRow(
                                number = "2",
                                title = "Check what to prepare",
                                description = "I'll show verified documents and steps."
                            )

                            HomeHowItWorksRow(
                                number = "3",
                                title = "Get ready to visit",
                                description = "I'll help with missing documents, problems and your final checklist."
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(9.dp))

                    OutlinedButton(
                        onClick = {
                            showServicePicker = !showServicePicker
                            showHowItWorks = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color(0xFF64B5F6)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1565C0)
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 Browse all services",
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = if (showServicePicker) "▲" else "▼",
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (showServicePicker) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            filteredServices.forEach { service ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showServicePicker = false
                                            showMainMenu = false
                                            serviceSearchText = ""
                                            showHowItWorks = false
                                            conversationState =
                                                ConversationState()
                                            sendMessage(service.serviceName)
                                        },
                                    shape = RoundedCornerShape(11.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF7FBFF)
                                    ),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 0.dp
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 9.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📄",
                                            fontSize = 16.sp
                                        )

                                        Spacer(
                                            modifier = Modifier.width(8.dp)
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = service.serviceName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1565C0)
                                            )

                                            Text(
                                                text =
                                                    "${service.department} • ${service.jurisdiction}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF607D8B)
                                            )
                                        }

                                        Text(
                                            text = "›",
                                            fontSize = 20.sp,
                                            color = Color(0xFF90A4AE)
                                        )
                                    }
                                }
                            }

                            if (filteredServices.isEmpty()) {
                                Text(
                                    text = "No matching service found. Try a different word.",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 8.dp,
                                            vertical = 12.dp
                                        ),
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    color = Color(0xFF607D8B)
                                )
                            }
                        }
                    }
                }
            }
        }

        /*
         * ------------------------------------------------
         * PHASE 8: SMART FEATURE DISCOVERY
         * ------------------------------------------------
         *
         * Show one noticeable "More help" entry while a service is active.
         * The complete capability set opens only when the citizen asks for it.
         */
        if (
            conversationState.serviceId != null &&
            conversationState.task == ConversationTask.SERVICE_ACTIONS &&
            !isProcessing
        ) {
            MoreHelpSection(
                expanded = showMoreHelp,
                onToggle = {
                    showMoreHelp = !showMoreHelp
                },
                onAction = { action ->
                    showMoreHelp = false
                    sendMessage(action)
                },
                hasDocumentContext = conversationState.documentId != null
            )
        }


        /*
         * ------------------------------------------------
         * PHASE 11: COMPACT QUICK OPTIONS
         * ------------------------------------------------
         *
         * Suggestions stay available, but they no longer consume several
         * vertical rows. A horizontal strip lets the citizen swipe through
         * all available suggestions while keeping the chat/input area large.
         */
        if (
            suggestions.isNotEmpty() &&
            !isProcessing
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 3.dp,
                        bottom = 5.dp
                    )
            ) {
                Text(
                    text = "Quick options  ›",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF486581),
                    modifier = Modifier.padding(
                        start = 4.dp,
                        bottom = 4.dp
                    )
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding = PaddingValues(
                        start = 2.dp,
                        end = 2.dp
                    )
                ) {
                    items(suggestions) { suggestion ->
                        OutlinedButton(
                            onClick = {
                                sendMessage(suggestion)
                            },
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = Color(0xFF90CAF9)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF1565C0)
                            ),
                            contentPadding = PaddingValues(
                                horizontal = 12.dp,
                                vertical = 4.dp
                            )
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        /*
         * ------------------------------------------------
         * INPUT AREA
         * ------------------------------------------------
         */

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            shape =
                RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.White
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 6.dp
                )
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            10.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {


                /*
                 * Text field.
                 */
                OutlinedTextField(

                    value = inputText,

                    onValueChange = {
                        inputText = it
                    },

                    modifier =
                        Modifier.weight(1f),

                    placeholder = {
                        Text(
                            text =
                                "Type your message..."
                        )
                    },

                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),

                    singleLine = false,

                    maxLines = 3
                )


                Spacer(
                    modifier =
                        Modifier.width(6.dp)
                )


                /*
                 * Voice language selector.
                 * This controls Android SpeechRecognizer only; the recognized
                 * Kannada or English text then enters the same AI + Room flow.
                 */
                Box {
                    OutlinedButton(
                        onClick = {
                            showSpeechLanguageMenu = true
                        },
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(10.dp, 0.dp),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFF90CAF9)
                        )
                    ) {
                        Text(
                            text = if (speechLanguage == "kn-IN") "ಕನ್ನಡ" else "EN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                    }

                    DropdownMenu(
                        expanded = showSpeechLanguageMenu,
                        onDismissRequest = {
                            showSpeechLanguageMenu = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("English")
                            },
                            onClick = {
                                speechLanguage = "en-IN"
                                showSpeechLanguageMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("ಕನ್ನಡ")
                            },
                            onClick = {
                                speechLanguage = "kn-IN"
                                showSpeechLanguageMenu = false
                            }
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.width(5.dp)
                )

                /*
                 * Microphone button.
                 */
                Button(

                    onClick = {

                        if (
                            isListening
                        ) {

                            speechRecognizer?.stopListening()

                            isListening = false

                        } else {

                            val permission =
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                )


                            if (
                                permission ==
                                PackageManager.PERMISSION_GRANTED
                            ) {

                                startSpeechRecognition(
                                    speechRecognizer,
                                    context,
                                    speechLanguage
                                )

                            } else {

                                microphonePermissionLauncher.launch(
                                    Manifest.permission.RECORD_AUDIO
                                )
                            }
                        }
                    },

                    modifier =
                        Modifier.size(52.dp),

                    shape = CircleShape,

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                if (isListening)
                                    Color(0xFFD32F2F)
                                else
                                    Color(0xFF1976D2)
                        ),

                    contentPadding =
                        PaddingValues(0.dp)
                ) {

                    Text(
                        text =
                            if (isListening)
                                "⏹"
                            else
                                "🎤",
                        fontSize = 22.sp
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(6.dp)
                )


                /*
                 * Send button.
                 */
                Button(

                    onClick = {

                        sendMessage(
                            inputText
                        )
                    },

                    modifier =
                        Modifier.size(52.dp),

                    shape = CircleShape,

                    enabled =
                        inputText.isNotBlank() &&
                                !isProcessing,

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF0D47A1)
                        ),

                    contentPadding =
                        PaddingValues(0.dp)
                ) {

                    Text(
                        text = "➤",
                        fontSize = 22.sp
                    )
                }
            }
        }
    }
}


/*
 * ------------------------------------------------
 * PHASE 10: HOME HOW-IT-WORKS ROW
 * ------------------------------------------------
 */
@Composable
fun HomeHowItWorksRow(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF7FAFC))
            .padding(
                horizontal = 9.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(25.dp)
                .clip(CircleShape)
                .background(Color(0xFFE3F2FD)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1565C0)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF17324D)
            )

            Text(
                text = description,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = Color(0xFF607D8B)
            )
        }
    }
}


/*
 * ------------------------------------------------
 * PHASE 9: NAVIGATION MENU ITEM
 * ------------------------------------------------
 */
@Composable
fun NavigationMenuItem(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = 8.dp,
                vertical = 9.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 18.sp,
            modifier = Modifier.width(30.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF17324D)
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF607D8B)
            )
        }

        Text(
            text = "›",
            fontSize = 22.sp,
            color = Color(0xFF90A4AE)
        )
    }
}


/*
 * ------------------------------------------------
 * PHASE 8: MORE HELP / FEATURE DISCOVERY
 * ------------------------------------------------
 *
 * Group the existing assistant capabilities into simple citizen-facing
 * categories instead of putting every feature permanently on screen.
 *
 * Document-help discovery is intentionally visible even when no document
 * is currently in context. The assistant then asks the citizen to name the
 * document, rather than hiding the capability.
 *
 * Phase 9 adds conversation navigation, a menu, a current-journey indicator,
 * and state restoration without deleting visible chat messages.
 *
 * Phase 10 improves the home screen with clearer service discovery,
 * local service search, and a simple three-step explanation of the app.
 *
 * Phase 11 keeps dynamic suggestions in a compact horizontal strip so the
 * response/chat area gets more vertical space without removing features.
 *
 * FINAL PHASE A combines the finished UI improvements with text-based
 * navigation commands and a stronger final readiness result card.
 */
@Composable
fun MoreHelpSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    onAction: (String) -> Unit,
    hasDocumentContext: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = 4.dp,
                bottom = 6.dp
            )
    ) {
        OutlinedButton(
            onClick = onToggle,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFF64B5F6)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (expanded) Color(0xFFEAF4FF) else Color.White,
                contentColor = Color(0xFF1565C0)
            ),
            contentPadding = PaddingValues(
                horizontal = 14.dp,
                vertical = 9.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⋮",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "More help",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (expanded)
                            "Choose from all available help options"
                        else
                            "See more things I can help you with",
                        fontSize = 11.sp,
                        color = Color(0xFF607D8B)
                    )
                }

                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 14.sp
                )
            }
        }

        if (expanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 3.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 390.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp)
                ) {
                    MoreHelpGroupTitle("⭐ Prepare")

                    MoreHelpActionGrid(
                        actions = listOf(
                            "Guide me",
                            "Required documents",
                            "Check my readiness",
                            "Show my checklist"
                        ),
                        onAction = onAction
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    MoreHelpGroupTitle("📄 Documents")

                    MoreHelpActionGrid(
                        actions = if (hasDocumentContext) {
                            listOf(
                                "Explain this document",
                                "Why do I need this document?",
                                "How do I obtain it?",
                                "Where can I get it?",
                                "Can I use another document?"
                            )
                        } else {
                            listOf(
                                "Explain this document",
                                "Why do I need this document?",
                                "I don't have a document",
                                "Required documents",
                                "Show my checklist"
                            )
                        },
                        onAction = onAction
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    MoreHelpGroupTitle("🏢 Application")

                    MoreHelpActionGrid(
                        actions = listOf(
                            "How to apply",
                            "Where to apply",
                            "Department / Office",
                            "Official website",
                            "Official helpline"
                        ),
                        onAction = onAction
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    MoreHelpGroupTitle("❗ Problems & Updates")

                    MoreHelpActionGrid(
                        actions = listOf(
                            "I have a problem",
                            "Application status",
                            "Renewal",
                            "Update / correction",
                            "Document lost"
                        ),
                        onAction = onAction
                    )
                }
            }
        }
    }
}


@Composable
private fun MoreHelpGroupTitle(
    title: String
) {
    Text(
        text = title,
        modifier = Modifier.padding(
            start = 4.dp,
            bottom = 5.dp
        ),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF37474F)
    )
}


@Composable
private fun MoreHelpActionGrid(
    actions: List<String>,
    onAction: (String) -> Unit
) {
    actions.chunked(2).forEach { rowActions ->
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            rowActions.forEach { action ->
                val isHelplineAction = action.equals("Official helpline", ignoreCase = true)

                OutlinedButton(
                    onClick = {
                        onAction(action)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = 2.dp,
                            end = 2.dp,
                            bottom = 4.dp
                        ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = if (isHelplineAction) 1.5.dp else 1.dp,
                        color = if (isHelplineAction) Color(0xFF1976D2) else Color(0xFFBBDEFB)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isHelplineAction) Color(0xFF1976D2) else Color(0xFFFAFCFF),
                        contentColor = if (isHelplineAction) Color.White else Color(0xFF1565C0)
                    ),
                    contentPadding = PaddingValues(
                        horizontal = 6.dp,
                        vertical = if (isHelplineAction) 8.dp else 7.dp
                    )
                ) {
                    Text(
                        text = if (isHelplineAction) "📞 Official helpline" else action,
                        fontSize = if (isHelplineAction) 11.sp else 11.sp,
                        fontWeight = if (isHelplineAction) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }

            if (rowActions.size == 1) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 2.dp, end = 2.dp)
                )
            }
        }
    }
}


/*
 * ------------------------------------------------
 * CHAT BUBBLE
 * ------------------------------------------------
 */

@Composable
fun ChatBubble(
    message: ChatMessage
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement =
            if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment =
                if (message.isUser) Alignment.End else Alignment.Start
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    start = 6.dp,
                    end = 6.dp,
                    bottom = 3.dp
                )
            ) {
                if (!message.isUser) {
                    Text(
                        text = "🤖",
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = "AI Service Guide",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF486581)
                    )
                } else {
                    Text(
                        text = "You",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF486581)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(
                    if (message.isUser) 0.78f else 0.92f
                ),
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (message.isUser) 18.dp else 5.dp,
                    bottomEnd = if (message.isUser) 5.dp else 18.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor =
                        if (message.isUser)
                            Color(0xFF1976D2)
                        else
                            Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation =
                        if (message.isUser) 1.dp else 2.dp
                )
            ) {
                if (message.isUser) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 13.dp
                        ),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = Color.White
                    )
                } else {
                    AssistantMessageContent(
                        text = message.text,
                        onOpenUrl = { url ->
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(url)
                                    )
                                )
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context,
                                    "Unable to open this link.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

/*
 * ------------------------------------------------
 * PHASE 9B: USER-FRIENDLY ASSISTANT RESPONSE RENDERER
 * ------------------------------------------------
 *
 * The verified response text is unchanged. Only the presentation changes:
 * - clear section headings
 * - status badges for REQUIRED / AVAILABLE / MISSING
 * - numbered steps shown as easy-to-scan rows
 * - bullets shown as compact information rows
 * - official URLs remain clickable
 * - normal paragraphs remain simple and readable
 */
@Composable
fun AssistantMessageContent(
    text: String,
    onOpenUrl: (String) -> Unit
) {
    val completedChecklistItems = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = Modifier.padding(
            horizontal = 14.dp,
            vertical = 13.dp
        ),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        text.lines().forEach { rawLine ->
            val line = rawLine.trim()

            if (line.isBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                return@forEach
            }

            val urlMatch = Regex(
                """https?://[^\s]+"""
            ).find(line)

            if (urlMatch != null && urlMatch.range.first == 0) {
                val url = urlMatch.value.trimEnd('.', ',', ';', ')')

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenUrl(url)
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEAF4FF)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = 11.dp,
                            vertical = 9.dp
                        )
                    ) {
                        Text(
                            text = "🔗 Open official source",
                            color = Color(0xFF1565C0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = url,
                            color = Color(0xFF1976D2),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                return@forEach
            }

            val isNumberedStep =
                Regex("""^(\d+)\.\s+(.+)""").find(line)

            val isBullet =
                line.startsWith("•") ||
                        line.startsWith("- ")

            val isRequired =
                line.contains("REQUIRED", ignoreCase = true) ||
                        line.contains("required for this service", ignoreCase = true)

            val isAvailable =
                line.contains("AVAILABLE", ignoreCase = true) ||
                        line.contains("marked", ignoreCase = true) &&
                        line.contains("available", ignoreCase = true)

            val isMissing =
                line.contains("MISSING", ignoreCase = true) ||
                        line.contains("do not have", ignoreCase = true) ||
                        line.contains("don't have", ignoreCase = true)

            val isReadySummary =
                line.startsWith("🎉 All required documents are currently ready", ignoreCase = true)

            val isNotReadySummary =
                line.contains("required document(s) are still missing", ignoreCase = true) ||
                        line.contains("required document is still missing", ignoreCase = true)

            val isHeading =
                line.startsWith("📝") ||
                        line.startsWith("📍") ||
                        line.startsWith("🔗") ||
                        line.startsWith("📞") ||
                        line.startsWith("🏛️") ||
                        line.startsWith("🏢") ||
                        line.startsWith("📋") ||
                        line.startsWith("⚠️") ||
                        line.startsWith("🔴") ||
                        line.startsWith("🟢") ||
                        line.startsWith("🟡") ||
                        line.startsWith("🔄") ||
                        line.startsWith("ℹ️") ||
                        line.startsWith("🎉") ||
                        line.startsWith("📘") ||
                        line.startsWith("❗") ||
                        line.equals("Final preparation checklist", ignoreCase = true) ||
                        line.equals("Simple explanation", ignoreCase = true)

            val isHelplineNumber =
                Regex(
                    """^(?:☎\s*)?(?:\+91[\s-]?)?\d[\d\s()/-]{6,}$"""
                ).matches(line)

            val readinessMatch =
                Regex("""^📊 Readiness score: (\d+)%$""").find(line)

            val isSmartChecklistItem =
                line.startsWith("☐ ") || line.startsWith("☑ ")

            when {
                readinessMatch != null -> {
                    val score = readinessMatch.groupValues[1].toIntOrNull() ?: 0
                    val scoreColor = when {
                        score >= 90 -> Color(0xFF2E7D32)
                        score >= 70 -> Color(0xFFEF6C00)
                        else -> Color(0xFFC62828)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF4F8FF)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFBBDEFB)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "PRE-VISIT READINESS",
                                        color = Color(0xFF1565C0),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.9.sp
                                    )
                                    Text(
                                        text = "How prepared are you?",
                                        color = Color(0xFF455A64),
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    text = "$score%",
                                    color = scoreColor,
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE3F2FD))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(score.coerceIn(0, 100) / 100f)
                                        .height(9.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(scoreColor)
                                )
                            }
                        }
                    }
                }

                isSmartChecklistItem -> {
                    val label = line.removePrefix("☐ ").removePrefix("☑ ").trim()
                    val checked = completedChecklistItems[label] == true || line.startsWith("☑ ")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (checked) Color(0xFFE8F5E9) else Color(0xFFF8FBFF)
                        ),
                        border = BorderStroke(1.dp, if (checked) Color(0xFFA5D6A7) else Color(0xFFBBDEFB))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    completedChecklistItems[label] = !checked
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { value ->
                                    completedChecklistItems[label] = value
                                }
                            )
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                color = if (checked) Color(0xFF2E7D32) else Color(0xFF263238),
                                fontSize = 12.sp,
                                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                isHelplineNumber -> {
                    val helplineNumber = line.removePrefix("☎").trim()
                    val helplineShape = RoundedCornerShape(14.dp)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = helplineShape,
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color(0xFF90CAF9)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(helplineShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFE3F2FD),
                                            Color(0xFFF7FBFF)
                                        )
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 12.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1976D2)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "☎",
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "OFFICIAL HELPLINE",
                                            color = Color(0xFF0D47A1),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp
                                        )

                                        Text(
                                            text = "Government service assistance",
                                            color = Color(0xFF546E7A),
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = Color(0xFFBBDEFB)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 12.dp,
                                                vertical = 9.dp
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = helplineNumber,
                                            modifier = Modifier.weight(1f),
                                            color = Color(0xFF0D47A1),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.3.sp
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = Color(0xFFE8F5E9)
                                        ) {
                                            Text(
                                                text = "✓ Official",
                                                modifier = Modifier.padding(
                                                    horizontal = 9.dp,
                                                    vertical = 5.dp
                                                ),
                                                color = Color(0xFF2E7D32),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(7.dp))

                                Text(
                                    text = "Use this official contact for service-related assistance.",
                                    color = Color(0xFF455A64),
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                /*
                 * Final readiness gets a larger visual result card.
                 */
                isReadySummary -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 12.dp
                            )
                        ) {
                            Text(
                                text = "✅ READY TO VISIT",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = "All required documents are currently ready.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF356859)
                            )
                        }
                    }
                }

                isNotReadySummary -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 12.dp
                            )
                        ) {
                            Text(
                                text = "⚠️ NOT READY YET",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = line,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF7A4B00)
                            )
                        }
                    }
                }

                /*
                 * Status lines get a compact badge instead of looking like
                 * an ordinary paragraph.
                 */
                isRequired -> {
                    ResponseStatusBadge(
                        text = line,
                        symbol = "🔴",
                        containerColor = Color(0xFFFFEBEE),
                        textColor = Color(0xFFC62828)
                    )
                }

                isAvailable -> {
                    ResponseStatusBadge(
                        text = line,
                        symbol = "🟢",
                        containerColor = Color(0xFFE8F5E9),
                        textColor = Color(0xFF2E7D32)
                    )
                }

                isMissing -> {
                    ResponseStatusBadge(
                        text = line,
                        symbol = "⚠️",
                        containerColor = Color(0xFFFFF3E0),
                        textColor = Color(0xFFE65100)
                    )
                }

                isHeading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF3F8FD)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = line,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = 8.dp
                                ),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF174A7C)
                        )
                    }
                }

                isNumberedStep != null -> {
                    val number = isNumberedStep.groupValues[1]
                    val content = isNumberedStep.groupValues[2]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 2.dp,
                                bottom = 2.dp
                            ),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(25.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE3F2FD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = number,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = content,
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 2.dp),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF243B53)
                        )
                    }
                }

                isBullet -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 4.dp,
                                top = 1.dp,
                                bottom = 1.dp
                            ),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )

                        Spacer(modifier = Modifier.width(7.dp))

                        Text(
                            text = line.removePrefix("•").removePrefix("- ").trim(),
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF334E68)
                        )
                    }
                }

                else -> {
                    Text(
                        text = line,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFF243B53)
                    )
                }
            }
        }
    }
}


/*
 * Small reusable status badge for important response states.
 */
@Composable
fun ResponseStatusBadge(
    text: String,
    symbol: String,
    containerColor: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = symbol,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.width(7.dp))

        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}


/*
 * ------------------------------------------------
 * START SPEECH RECOGNITION
 * ------------------------------------------------
 */

fun startSpeechRecognition(
    speechRecognizer: SpeechRecognizer?,
    context: android.content.Context,
    languageTag: String = "en-IN"
) {

    if (
        speechRecognizer == null
    ) {

        Toast.makeText(
            context,
            "Speech recognition is not available on this device.",
            Toast.LENGTH_LONG
        ).show()

        return
    }


    try {

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    languageTag
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    languageTag
                )

                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    3
                )

                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    true
                )

                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    if (languageTag == "kn-IN") {
                        "ನಿಮ್ಮ ಪ್ರಶ್ನೆ ಅಥವಾ ಉತ್ತರವನ್ನು ಹೇಳಿ..."
                    } else {
                        "Speak your question or answer..."
                    }
                )
            }


        speechRecognizer.startListening(
            intent
        )

    } catch (exception: Exception) {

        Toast.makeText(
            context,
            "Could not start microphone: ${exception.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}

