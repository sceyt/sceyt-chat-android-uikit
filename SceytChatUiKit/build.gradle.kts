import com.sceyt.chat.MainGradlePlugin

plugins {
    `android-library`
    `kotlin-android`
    `kotlin-kapt`
}

apply<MainGradlePlugin>()
apply(from = "${rootProject.projectDir}/maven-publish/publish-module.gradle")

android {
    namespace = "com.sceyt.chatuikit"

    defaultConfig {

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    buildTypes {
        getByName("release") {
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        dataBinding = true
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
    api(libs.play.services.base)
    annotationProcessor(libs.room.compiler)
    //noinspection KaptUsageInsteadOfKsp
    kapt(libs.room.compiler)
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
    api(libs.light.compressor)
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
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)

    // Koin testing tools
    testImplementation(libs.koin.test)
    androidTestImplementation(libs.koin.test)
    // Needed JUnit version
    testImplementation(libs.koin.test.junit4)
    androidTestImplementation(libs.koin.test.junit4)
}