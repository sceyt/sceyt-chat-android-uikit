import com.sceyt.chat.Config
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    compileSdk = Config.compileSdk
    namespace = "com.sceyt.chat.call"

    defaultConfig {
        minSdk = Config.minSdk
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    implementation(project(":SceytChatUiKit"))

    api(libs.sceyt.call.android.sdk)
    implementation(libs.sceyt.tonemanager.android.sdk)

    // WorkManager
    implementation(libs.work.runtime.ktx)
    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    // Firebase Messaging
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    // Jetpack Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    debugImplementation(libs.ui.tooling)
    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)
    implementation(libs.kotlinx.collections.immutable)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.inline)
}
