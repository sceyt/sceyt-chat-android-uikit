import com.sceyt.chat.MainGradlePlugin
import com.sceyt.chat.configureMavenPublishing
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

apply<MainGradlePlugin>()
configureMavenPublishing(
    artifactId = "sceyt-chat-connection",
    artifactDescription = "Sceyt Chat connection and lifecycle management for Android"
)

android {
    namespace = "com.sceyt.chat.connection"

    buildFeatures {
        buildConfig = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    api(libs.sceyt.chat.android.sdk)
    api(libs.lifecycle.runtime.ktx)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.core.ktx)
    implementation(libs.gson)
    implementation(libs.lifecycle.process)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.core.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}
