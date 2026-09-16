package com.sceyt.chat

@Suppress("ConstPropertyName", "unused")
object Config {
    const val compileSdk = 36
    const val targetSdk = 36
    const val minSdk = 24

    /** MavenCentral */
    const val mavenCentralGroup = "com.sceyt"

    object UiKit {
        const val artifactId = "sceyt-chat-android-uikit"
        const val description = "Sceyt Chat Android UIKit"

        // const val version = "2.1.5"
        // const val version = "local"
        const val version = "2.1.523025-SNAPSHOT"
    }

    object ChatConnection {
        const val artifactId = "sceyt-chat-connection"
        const val description = "Sceyt Chat connection and lifecycle management for Android"

        // const val version = "local"
        const val version = "1.0.0-SNAPSHOT"
    }

    /** App version */
    const val versionCode = 62
    const val versionName = "1.3.2"
}
