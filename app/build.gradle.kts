plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.phishguard.jev"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.phishguard.jev"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Downloads Archivo at runtime via Google Play Services Fonts, instead of
    // bundling font files in the APK.
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // Network client for calling the TypeSafe System One endpoint directly.
    // (Deliberately not pulling in the community JitPack Kotlin SDK for a one-day
    // build — one fewer unknown dependency to debug. Swap in typesafe-sdk-kotlin
    // later if you want retries/backoff for free.)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // org.json is bundled with the Android platform — no dependency needed.
}
