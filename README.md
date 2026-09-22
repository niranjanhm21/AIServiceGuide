# AI Service Guide

An Android-based citizen service assistance application that helps users understand government/public-service procedures, required documents, missing documents, document alternatives, where to obtain documents, and related service guidance **before visiting an office**.

The application combines a verified local database with Gemini-powered natural-language understanding. Gemini is used to understand the citizen's message and conversation context, while the local Room database remains the source of truth for service and document information.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Problem Statement](#problem-statement)
- [Objectives](#objectives)
- [Key Features](#key-features)
- [How the Application Works](#how-the-application-works)
- [Technology Stack](#technology-stack)
- [Application Architecture](#application-architecture)
- [Major Components](#major-components)
- [Conversation and AI Flow](#conversation-and-ai-flow)
- [Supported Interaction Types](#supported-interaction-types)
- [Multilingual Support](#multilingual-support)
- [Database Design](#database-design)
- [Project Structure](#project-structure)
- [Requirements](#requirements)
- [Setup and Installation](#setup-and-installation)
- [Gemini API Configuration](#gemini-api-configuration)
- [Running the Application](#running-the-application)
- [Testing](#testing)
- [Git and GitHub](#git-and-github)
- [Security](#security)
- [Future Enhancements](#future-enhancements)
- [Academic Project Information](#academic-project-information)
- [License](#license)

---

## Project Overview

**AI Service Guide** is an Android application designed to reduce confusion and unnecessary visits to government/public-service offices.

A citizen can describe what they want in natural language, for example:

> "I need an income certificate."

The application identifies the service and can then guide the user through:

- Required documents
- Application procedure
- Checklist preparation
- Missing documents
- How to obtain a missing document
- Where to obtain a document
- Office/service information
- Application-related problems
- Starting another service
- Contextual follow-up questions

The application is designed around a **database-first approach**:

> **AI understands the citizen. The verified application database provides the factual service information.**

---

## Problem Statement

Citizens often need to visit government offices multiple times because they may not know:

- Which documents are required
- Whether a particular document is mandatory
- What to do when a document is missing
- Where a document can be obtained
- Which office or department handles a service
- What preparation is required before visiting
- How to continue when they have a problem during an application

This project provides a conversational interface that helps users prepare before visiting the relevant service office.

---

## Objectives

1. Provide a simple conversational interface for citizen services.
2. Understand natural-language requests instead of requiring fixed commands.
3. Support English, Kannada, and Kannada-English mixed communication.
4. Use a verified local database for service/document information.
5. Maintain conversation context for follow-up questions.
6. Help users identify missing documents.
7. Provide contextual guidance for obtaining missing documents.
8. Generate a practical checklist before the user visits an office.
9. Reduce unnecessary confusion and repeated visits.
10. Provide a scalable architecture for adding additional services.

---

## Key Features

### 1. Natural-Language Service Identification

Users can describe their requirement naturally.

Example:

```text
I need an income certificate.
```

The AI identifies the relevant service from the services available in the application database.

---

### 2. Document Requirement Guidance

Users can ask:

```text
What documents are required?
```

or in Kannada:

```text
ಅಗತ್ಯ ದಾಖಲೆಗಳು ಯಾವುವು?
```

The application retrieves the relevant information from the local database.

---

### 3. Missing Document Assistance

Example:

```text
I don't have address proof.
```

The application can identify the missing document and guide the user through the next available steps.

---

### 4. Document Follow-Up Questions

The conversation supports contextual questions such as:

```text
How do I obtain it?
```

```text
Where can I get it?
```

```text
Can I use another document?
```

The user does not always need to repeat the document or service name.

---

### 5. Application Procedure

Users can ask:

```text
How do I apply?
```

or:

```text
ಅರ್ಜಿ ಹೇಗೆ ಸಲ್ಲಿಸಬೇಕು?
```

The application identifies the requested action and uses verified application data.

---

### 6. Checklist

The application can guide the citizen toward a final preparation checklist.

Example:

```text
Show my checklist.
```

or:

```text
ಚೆಕ್ ಲಿಸ್ಟ್ ತೋರಿಸಿ
```

---

### 7. Context-Aware Conversation

The application maintains conversation state.

For example:

```text
User:
I need an income certificate.

Assistant:
Income Certificate selected.

User:
What documents are required?

Assistant:
[requirements]

User:
I don't have address proof.

Assistant:
[missing document guidance]

User:
How do I get it?

Assistant:
[contextual guidance for address proof]
```

The final question is interpreted using the existing conversation context.

---

### 8. Kannada Support

The application supports Kannada-script requests such as:

```text
ಅಗತ್ಯ ದಾಖಲೆಗಳು ಯಾವುವು
```

```text
ಅರ್ಜಿ ಹೇಗೆ ಸಲ್ಲಿಸಬೇಕು
```

```text
ನನ್ನ ಬಳಿ ವಿಳಾಸದ ಪುರಾವೆ ಇಲ್ಲ
```

```text
ಇದನ್ನು ಹೇಗೆ ಪಡೆಯುವುದು
```

```text
ಇದು ಎಲ್ಲಿ ಸಿಗುತ್ತದೆ
```

---

### 9. Kannada-English Mixed Language

The application is designed to handle natural mixed-language input where users combine Kannada and English terms.

Example:

```text
income certificate ಗೆ ಯಾವ documents ಬೇಕು?
```

---

### 10. Voice Input

The application supports voice-based interaction so users can communicate without always typing their requests.

---

## How the Application Works

The high-level flow is:

```text
                 ┌─────────────────────┐
                 │       Citizen       │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Android Chat UI     │
                 │ Jetpack Compose     │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ ServiceAssistant    │
                 │ Conversation Logic  │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Gemini Intent       │
                 │ Analyzer            │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Intent / Action     │
                 │ + Context           │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Room Database       │
                 │ Source of Truth     │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │ Verified Citizen    │
                 │ Guidance            │
                 └─────────────────────┘
```

---

## Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Primary programming language |
| Android Studio | Development environment |
| Jetpack Compose | Modern Android UI |
| Room | Local database |
| Gemini API | Natural-language understanding |
| Kotlin Coroutines | Asynchronous operations |
| Android Voice Input | Voice interaction |
| Gradle Kotlin DSL | Build configuration |
| Git | Version control |
| GitHub | Remote source-code repository |
| Pixel 9 Emulator | Development/testing device |

---

## Application Architecture

The project follows a layered approach.

### Presentation Layer

Responsible for:

- Chat interface
- User messages
- Assistant messages
- Suggested actions
- Voice input
- Conversation display
- Service selection
- Checklist display

Main UI entry point:

```text
MainActivity.kt
```

---

### Conversation / Business Logic Layer

The main conversation controller is:

```text
ServiceAssistant.kt
```

Responsibilities include:

- Processing user messages
- Maintaining conversation state
- Handling service selection
- Handling document checks
- Handling missing documents
- Handling contextual follow-ups
- Managing conversation tasks
- Connecting AI understanding with verified database data

---

### AI Understanding Layer

The AI natural-language understanding component is:

```text
GeminiIntentAnalyzer.kt
```

Gemini is used to extract information such as:

- Service
- Intent
- Document
- Problem
- Desired action
- Confidence

The AI is explicitly separated from factual government-service data.

---

### Data Layer

The Room database contains service and document information.

Important components include:

```text
AppDatabase
DatabaseProvider
DatabaseSeeder
Service
ServiceDao
Document
DocumentDao
ServiceDocument
ServiceDocumentDao
Problem
ProblemDao
ServiceProcedure
```

---

## Major Components

### `MainActivity.kt`

Provides the main Android user interface and connects the UI with the service assistant.

---

### `ServiceAssistant.kt`

Controls the application's conversational workflow.

The application uses conversation tasks such as:

```text
CHOOSE_SERVICE
SERVICE_ACTIONS
PROBLEM_SELECTION
CHECK_DOCUMENT
DEATH_CONTEXT
MISSING_DOCUMENT
FINISHED
```

---

### `ConversationModels.kt`

Contains shared conversation models, including:

```text
ChatMessage
ConversationTask
ConversationState
AssistantResult
```

Keeping these models separate prevents duplicate class definitions between `MainActivity.kt` and `ServiceAssistant.kt`.

---

### `GeminiIntentAnalyzer.kt`

Converts natural-language messages into structured intent information.

The analyzer supports intents including:

```text
IDENTIFY_SERVICE
APPLICATION_PROCEDURE
DOCUMENT_REQUIREMENT
MISSING_DOCUMENT
DOCUMENT_ALTERNATIVE
HOW_TO_OBTAIN_DOCUMENT
WHERE_TO_OBTAIN_DOCUMENT
OFFICE_LOCATION
DEPARTMENT
APPLICATION_PROBLEM
REJECTION
STATUS
CHECKLIST
START_ANOTHER_SERVICE
GENERAL_HELP
```

---

### `ServiceProcedure.kt`

Stores/represents service procedure information used by the application.

---

### `DatabaseSeeder.kt`

Provides initial service/document data for the local Room database.

---

## Conversation and AI Flow

The application follows this principle:

```text
User Message
     ↓
Understand Intent
     ↓
Resolve Current Conversation Context
     ↓
Identify Service / Document / Action
     ↓
Verify Against Room Database
     ↓
Generate Response
```

Gemini is **not** treated as the authoritative source for government requirements.

The application uses the database to determine what information can actually be shown to the citizen.

---

## Supported Interaction Types

The application can handle interactions such as:

### Service Identification

```text
I need a caste certificate.
```

### Requirements

```text
What documents do I need?
```

### Application Procedure

```text
How do I apply?
```

### Missing Document

```text
I don't have address proof.
```

### How to Obtain

```text
How do I get it?
```

### Where to Obtain

```text
Where can I get it?
```

### Alternative Document

```text
Can I use another document?
```

### Checklist

```text
Show my checklist.
```

### Problem

```text
My application was rejected.
```

### Another Service

```text
I want to apply for another service.
```

---

## Multilingual Support

The application is designed for:

- English
- Kannada
- Kannada-English mixed language

The response language can follow the language used by the citizen.

Example:

```text
User:
ಅಗತ್ಯ ದಾಖಲೆಗಳು ಯಾವುವು?

Assistant:
[Kannada response]
```

Mixed-language example:

```text
User:
income certificate ಗೆ ಯಾವ documents ಬೇಕು?

Assistant:
[Context-aware response]
```

---

## Database Design

The application uses Room for local structured data.

The main relationships conceptually follow:

```text
Service
   │
   ├───────────────┐
   │               │
   ▼               ▼
Documents       Procedures
   │
   ▼
ServiceDocument
```

Additional service-related information can be represented through problem records and related DAOs.

### Database-first principle

The database is responsible for factual application information such as:

- Available services
- Documents
- Service-document relationships
- Procedures
- Problems and related guidance

The AI layer interprets the citizen's request and maps it to this information.

---

## Project Structure

A simplified project structure is:

```text
AIServiceGuide/
│
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/
│   │       │       └── example/
│   │       │           └── aiserviceguide/
│   │       │               │
│   │       │               ├── ai/
│   │       │               │   └── GeminiIntentAnalyzer.kt
│   │       │               │
│   │       │               ├── data/
│   │       │               │   ├── AppDatabase.kt
│   │       │               │   ├── ConversationModels.kt
│   │       │               │   ├── DatabaseProvider.kt
│   │       │               │   ├── DatabaseSeeder.kt
│   │       │               │   ├── Document.kt
│   │       │               │   ├── DocumentDao.kt
│   │       │               │   ├── Problem.kt
│   │       │               │   ├── ProblemDao.kt
│   │       │               │   ├── Service.kt
│   │       │               │   ├── ServiceAssistant.kt
│   │       │               │   ├── ServiceDao.kt
│   │       │               │   ├── ServiceDocument.kt
│   │       │               │   ├── ServiceDocumentDao.kt
│   │       │               │   └── ServiceProcedure.kt
│   │       │               │
│   │       │               └── MainActivity.kt
│   │       │
│   │       └── res/
│   │
│   ├── build.gradle.kts
│   └── ...
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

The exact project tree may contain additional Android Studio-generated files and resources.

---

## Requirements

To build the project locally, you need:

- Android Studio
- Android SDK
- Kotlin support through Android Studio
- Gradle/Gradle Wrapper
- An Android emulator or physical Android device
- Internet access for dependency/API operations
- A valid Gemini API configuration for AI functionality

---

## Setup and Installation

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/AIServiceGuide.git
```

Then:

```bash
cd AIServiceGuide
```

### 2. Open the project

Open the project in Android Studio.

Allow Android Studio/Gradle to synchronize the project.

### 3. Configure the Gemini API key

Configure the API key using your project's existing secret/BuildConfig mechanism.

**Do not place a real API key directly inside a committed Kotlin source file.**

### 4. Sync Gradle

In Android Studio:

```text
File → Sync Project with Gradle Files
```

### 5. Build

Use:

```text
Build → Make Project
```

or:

```text
Build → Rebuild Project
```

### 6. Run

Select an emulator/device and run:

```text
Run → Run 'app'
```

---

## Gemini API Configuration

The application uses Gemini for natural-language understanding.

The intended responsibility of Gemini is:

```text
Natural-language message
        ↓
Intent understanding
        ↓
Structured result
        ↓
Room database verification
        ↓
Citizen response
```

Gemini should not be used as an unrestricted source of government requirements.

### Important security rule

Never commit:

```text
API keys
Passwords
Access tokens
Private credentials
Keystore passwords
```

to GitHub.

GitHub explicitly recommends not pushing API keys or other sensitive credentials to repositories. Use ignored local configuration or an appropriate secret-management mechanism instead.

---

## Running the Application

After configuration:

1. Start the Android emulator or connect a device.
2. Build the project.
3. Run the application.
4. Open the chat interface.
5. Try a service request.
6. Continue with document/procedure questions.
7. Test Kannada and mixed-language messages.
8. Test missing-document follow-ups.
9. Test voice input.

---

## Testing

Suggested test cases:

### English

```text
I need an income certificate.
```

```text
What documents are required?
```

```text
How do I apply?
```

```text
I don't have address proof.
```

```text
How do I get it?
```

```text
Where can I get it?
```

---

### Kannada

```text
ಆದಾಯ ಪ್ರಮಾಣ ಪತ್ರ ಬೇಕು
```

```text
ಅಗತ್ಯ ದಾಖಲೆಗಳು ಯಾವುವು?
```

```text
ಅರ್ಜಿ ಹೇಗೆ ಸಲ್ಲಿಸಬೇಕು?
```

```text
ನನ್ನ ಬಳಿ ವಿಳಾಸದ ಪುರಾವೆ ಇಲ್ಲ
```

```text
ಇದನ್ನು ಹೇಗೆ ಪಡೆಯುವುದು?
```

```text
ಇದು ಎಲ್ಲಿ ಸಿಗುತ್ತದೆ?
```

```text
ಚೆಕ್ ಲಿಸ್ಟ್ ತೋರಿಸಿ
```

---

### Mixed Language

```text
income certificate ಗೆ ಯಾವ documents ಬೇಕು?
```

```text
address proof ಇಲ್ಲ, ಏನು ಮಾಡಬೇಕು?
```

---

## Git and GitHub

The project uses Git for local version control and GitHub for remote source-code storage.

### Check whether Git is already initialized

Open a terminal in the project root:

```bash
git status
```

If Git is already configured, you should see the current branch and changed files.

You can also check:

```bash
git remote -v
```

If an `origin` URL is already shown, the project is already connected to a GitHub remote.

---

### First-time Git setup

If the project has never been initialized as a Git repository:

```bash
git init -b main
```

Then:

```bash
git add .
```

Commit:

```bash
git commit -m "Initial commit"
```

GitHub's documented workflow for an existing local project is to initialize Git if needed, commit the project, add the GitHub remote, and push the branch. 

---

### Create the GitHub repository

On GitHub:

1. Create a new repository.
2. Give it a name such as:

```text
AIServiceGuide
```

3. Choose Public or Private.
4. If the project already contains its own local history, do **not** initialize the new GitHub repository with another README, license, or `.gitignore`.
5. Create the repository.

GitHub provides the repository's remote URL after creation. 

---

### Connect your local project to GitHub

Copy your repository URL and run:

```bash
git remote add origin https://github.com/YOUR_USERNAME/AIServiceGuide.git
```

Verify:

```bash
git remote -v
```

You should see:

```text
origin  https://github.com/YOUR_USERNAME/AIServiceGuide.git (fetch)
origin  https://github.com/YOUR_USERNAME/AIServiceGuide.git (push)
```

Then push:

```bash
git push -u origin main
```

This is the standard GitHub workflow for pushing an existing local repository. 

---

## Updating GitHub After Future Changes

Whenever you modify the project:

### 1. Check changes

```bash
git status
```

### 2. Review changes

```bash
git diff
```

### 3. Stage changes

```bash
git add .
```

### 4. Commit

Use a meaningful message:

```bash
git commit -m "Improve Kannada conversation handling"
```

### 5. Push

```bash
git push
```

Your changes will then be uploaded to the GitHub repository.

---

## Useful Git Commands

### Check status

```bash
git status
```

### View commit history

```bash
git log --oneline
```

### Check remote repository

```bash
git remote -v
```

### Download latest remote changes

```bash
git pull
```

### Push local commits

```bash
git push
```

### Create a new branch

```bash
git checkout -b feature-name
```

### Return to main

```bash
git checkout main
```

---

## `.gitignore`

Android Studio projects should not commit generated/build files or local machine-specific configuration.

A suitable Android `.gitignore` should normally exclude items such as:

```gitignore
# Gradle
.gradle/
build/
**/build/

# Android Studio
.idea/
*.iml

# Local configuration
local.properties

# Generated files
captures/
.externalNativeBuild/
.cxx/

# APKs
*.apk
*.aab

# Logs
*.log

# OS files
.DS_Store
Thumbs.db

# Secrets
*.env
secrets.properties
```

Do not blindly add a file to `.gitignore` if your project intentionally needs it. Review the files before the first push.

GitHub recommends using `.gitignore` to prevent files that should not be committed from being added to the repository. citeturn0search1

---

## Security

### Never upload the Gemini API key

Before pushing:

```bash
git status
```

and inspect staged files.

Also search your project for:

```text
AIza
GEMINI_API_KEY
API_KEY
password
secret
token
```

A real credential should not appear in the GitHub repository.

GitHub specifically warns against committing API keys, tokens, passwords, and other credentials—even to private repositories. citeturn0search0turn0search15

If a secret has already been committed, simply deleting it in a later commit does not make the old secret safe; the credential should be rotated and the repository history handled appropriately.

---

## Future Enhancements

Potential future improvements include:

- More government services
- More document types
- More regional languages
- Better voice interaction
- Offline-first improvements
- More detailed application-status guidance
- Government-service API integration where officially available
- Accessibility improvements
- Improved service search
- More advanced conversation analytics
- Administrative tools for maintaining verified service data
- Better multilingual response generation
- Unit and UI test coverage
- Automated CI/CD builds through GitHub Actions

---

## Academic Project Information

**Project Name:** AI Service Guide

**Platform:** Android

**Primary Language:** Kotlin

**Project Type:** MCA Mini/Academic Project

**Domain:** AI + Android + Citizen Services / GovTech

### Core Technologies

```text
Android
Kotlin
Jetpack Compose
Room Database
Gemini API
Kotlin Coroutines
Voice Input
Git
GitHub
```

### Core Concept

The project combines:

```text
Conversational AI
        +
Verified Local Database
        +
Context-Aware Conversation
        +
Multilingual Interaction
        =
Citizen Service Guidance
```

---

## Design Principle

The most important architectural principle of this project is:

> **AI interprets the user's request; verified application data determines the factual answer.**

This reduces the risk of allowing a generative AI model to independently invent government-service requirements.

---

## Project Status

The application currently has:

- Android application structure
- Jetpack Compose interface
- Room database
- Service/document models
- Database seeding
- Gemini intent analysis
- Context-aware conversation
- Missing-document flow
- Checklist flow
- Kannada interaction support
- English interaction support
- Kannada-English mixed-language handling
- Voice input
- Git/GitHub version-control workflow

---

## Author

Developed as an MCA academic project.

---

## License

If this project is intended for academic submission only, a license can be added later according to the project's intended distribution.

If you plan to make the repository public and allow others to reuse the code, choose an appropriate open-source license before publishing.
