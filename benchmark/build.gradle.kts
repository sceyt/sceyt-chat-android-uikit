import com.sceyt.chat.Config
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.benchmark)
}

android {
    namespace = "com.sceyt.chatuikit.benchmark"
    compileSdk = Config.compileSdk
    testBuildType = "release"

    defaultConfig {
        minSdk = Config.minSdk
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
    }

    buildFeatures {
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    androidTestImplementation(project(":SceytChatUiKit"))
    androidTestImplementation(libs.benchmark.junit4)
    androidTestImplementation(libs.junit.ktx)
}
