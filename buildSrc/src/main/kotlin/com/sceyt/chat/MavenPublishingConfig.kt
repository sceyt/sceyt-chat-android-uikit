package com.sceyt.chat

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import java.io.File
import java.util.Properties

fun Project.configureMavenPublishing() {
    plugins.apply("com.vanniktech.maven.publish")
    plugins.apply("maven-publish")

    // First try to copy credentials from local.properties to gradle.properties
    copyCredentialsToGradleProperties(this)

    configure<MavenPublishBaseExtension> {
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

        if (Config.mavenCentralVersion.contains("-SNAPSHOT")) {
            publishSnapshotToMavenCentral()
        } else {
            // For release versions, use the Central Portal
            publishToMavenCentral(true)
        }

        signAllPublications()
    }
}

private fun Project.publishSnapshotToMavenCentral() {
    // For SNAPSHOT versions, we need to manually add the repository
    afterEvaluate {
        // Access the publishing extension after plugins are applied
        val publishing = project.extensions.getByType<PublishingExtension>()
        publishing.repositories {
            maven {
                name = "sonatypeSnapshot"
                setUrl("https://central.sonatype.com/repository/maven-snapshots/")
                credentials {
                    username = project.findProperty("mavenCentralUsername") as String?
                    password = project.findProperty("mavenCentralPassword") as String?
                }
            }
        }
    }
}

private fun copyCredentialsToGradleProperties(project: Project) {
    try {
        // Load properties from local.properties
        val localProperties = Properties()
        val localPropertiesFile = File(project.rootProject.rootDir, "local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        } else {
            project.logger.warn("local.properties file not found")
            return
        }

        // Check if gradle.properties exists in user home
        val userGradlePropertiesFile = File(System.getProperty("user.home"), ".gradle/gradle.properties")
        if (!userGradlePropertiesFile.exists()) {
            // Create directory if it doesn't exist
            userGradlePropertiesFile.parentFile.mkdirs()
            // Create the file
            userGradlePropertiesFile.createNewFile()
        }

        // Load existing gradle.properties
        val gradleProperties = Properties()
        userGradlePropertiesFile.inputStream().use { gradleProperties.load(it) }

        // Check if we need to update gradle.properties
        var needsUpdate = false
        val propertiesToCopy = listOf(
            "mavenCentralUsername",
            "mavenCentralPassword",
            "signing.keyId",
            "signing.password",
            "signing.secretKeyRingFile"
        )

        // Copy missing properties
        for (prop in propertiesToCopy) {
            val value = localProperties.getProperty(prop)
            if (value != null && gradleProperties.getProperty(prop) == null) {
                gradleProperties.setProperty(prop, value)
                needsUpdate = true
            }
        }

        project.logger.lifecycle("Gradle properties need update: $needsUpdate")

        // Update gradle.properties if needed
        if (needsUpdate) {
            userGradlePropertiesFile.outputStream().use {
                gradleProperties.store(it, "Updated by Sceyt build script")
            }
            project.logger.lifecycle("Updated gradle.properties with Maven publishing credentials")
        }
    } catch (e: Exception) {
        project.logger.error("Error copying credentials to gradle.properties: ${e.message}")
    }
} 