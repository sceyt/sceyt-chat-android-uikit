import com.sceyt.chat.Config
import com.sceyt.chat.MainGradlePlugin
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("androidx.room")
    id("com.vanniktech.maven.publish") version "0.28.0"
    `maven-publish`
}

apply<MainGradlePlugin>()

mavenPublishing {
    coordinates(
        groupId = Config.mavenCentralGroup,
        artifactId = Config.mavenCentralArtifactId,
        version = Config.mavenCentralVersion
    )

    pom {
        name.set(Config.mavenCentralArtifactId)
        description.set("Sceyt Chat Android UIKit")
        url.set("https://github.com/sceyt/sceyt-chat-android-uikit")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/sceyt/sceyt-chat-android-uikit/blob/master/LICENSE")
            }
        }

        developers {
            developer {
                id.set("maratsct")
                name.set("Marat Hambikyan")
                email.set("marat@sceyt.com")
            }
        }

        scm {
            connection.set("scm:git:github.com/sceyt/sceyt-chat-android-uikit.git")
            developerConnection.set("scm:git:ssh://github.com/sceyt/sceyt-chat-android-uikit.git")
            url.set("https://github.com/sceyt/sceyt-chat-android-uikit/tree/master")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}

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

val mockitoAgent = configurations.create("mockitoAgent")
tasks.withType<Test>().configureEach {
    // Find the specific byte-buddy-agent JAR from the configuration
    val byteBuddyAgentJar = mockitoAgent.files.find { it.name.startsWith("byte-buddy-agent") }

    // Ensure the JAR was found before adding the jvmArgs
    if (byteBuddyAgentJar != null) {
        @Suppress("USELESS_ELVIS")
        jvmArgs = (jvmArgs ?: emptyList()) + "-javaagent:${byteBuddyAgentJar.absolutePath}"
    } else {
        // Log a warning or error if the byte-buddy-agent JAR is not found
        logger.warn("Byte-buddy-agent JAR not found in the 'mockitoAgent' configuration.")
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

    mockitoAgent(libs.mockito.inline)
}