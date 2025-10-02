package com.sceyt.chat

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

fun Project.configureMockitoAgent(): Configuration {
    val mockitoAgent = configurations.create("mockitoAgent")

    tasks.withType<Test>().configureEach {
        // Find the specific byte-buddy-agent JAR from the configuration
        val byteBuddyAgentJar = mockitoAgent.files.find { it.name.startsWith("byte-buddy-agent") }

        // Ensure the JAR was found before adding the jvmArgs
        if (byteBuddyAgentJar != null) {
            jvmArgs = jvmArgs.orEmpty().plus("-javaagent:${byteBuddyAgentJar.absolutePath}")
        } else {
            // Log a warning or error if the byte-buddy-agent JAR is not found
            logger.warn("Byte-buddy-agent JAR not found in the 'mockitoAgent' configuration.")
        }
    }

    return mockitoAgent
} 