import com.sceyt.chat.MainGradlePlugin
import com.sceyt.chat.configureMavenPublishing
import com.sceyt.chat.configureMockitoAgent

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

apply<MainGradlePlugin>()
configureMavenPublishing()
val mockitoAgent = configureMockitoAgent()

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.sceyt.chatuikit"

    buildTypes {
        getByName("release") {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(libs.appcompat)
    api(libs.material)
    api(libs.constraintlayout)
    api(libs.recyclerview)
    api(libs.fragment.ktx)
    api(libs.core.ktx)
    api(libs.sceyt.chat.android.sdk)
    api(libs.lifecycle.runtime.ktx)
    api(libs.lifecycle.livedata.ktx)
    api(libs.glide)
    api(libs.glide.transformations)
    api(libs.ion)
    api(libs.firebase.messaging.ktx)
    api(libs.lifecycle.process)
    ksp(libs.room.compiler)
    api(libs.room.runtime)
    api(libs.room.ktx)
    api(libs.koin.android)
    api(libs.media3.exoplayer)
    api(libs.media3.ui)
    api(libs.lottie)
    api(libs.jsoup)
    api(libs.flexbox)
    api(libs.sdp.android)
    api(libs.ssp.android)
    api(libs.work.runtime.ktx)
    api(libs.waveformSeekBar)
    api(libs.photo.view)
    api(libs.emoji2.bundled)
    api(libs.emoji.google)
    api(libs.libphonenumber)
    api(libs.ucrop)

    // Overriding the version of the library
    implementation(libs.gson)
    implementation(libs.okio)

    // Instrumented Unit Tests
    androidTestImplementation(libs.junit.ktx)
    androidTestImplementation(libs.core.testing)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.truth)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.core.testing)
    testImplementation(libs.robolectric)
    mockitoAgent(libs.mockito.inline)

    // Koin testing tools
    testImplementation(libs.koin.test)
    androidTestImplementation(libs.koin.test)
    // Needed JUnit version
    testImplementation(libs.koin.test.junit4)
    androidTestImplementation(libs.koin.test.junit4)
}