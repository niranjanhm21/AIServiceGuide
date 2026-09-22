import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.aiserviceguide"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.aiserviceguide"

        minSdk = 27
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")

        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use {
                localProperties.load(it)
            }
        }

        val geminiApiKey =
            localProperties.getProperty("GEMINI_API_KEY", "")

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"$geminiApiKey\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    // ----------------------------------------------------
    // Jetpack Compose
    // ----------------------------------------------------

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.material:material-icons-core")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)


    // ----------------------------------------------------
    // Room 3 Database
    // ----------------------------------------------------

    implementation(libs.androidx.room.runtime)

    ksp(libs.androidx.room.compiler)


    // ----------------------------------------------------
    // SQLite
    // ----------------------------------------------------

    implementation(libs.androidx.sqlite.bundled)


    // ----------------------------------------------------
    // Unit Tests
    // ----------------------------------------------------

    testImplementation(libs.junit)


    // ----------------------------------------------------
    // Android Tests
    // ----------------------------------------------------

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)


    // ----------------------------------------------------
    // Compose Debug Tools
    // ----------------------------------------------------

    debugImplementation(libs.androidx.compose.ui.tooling)
}