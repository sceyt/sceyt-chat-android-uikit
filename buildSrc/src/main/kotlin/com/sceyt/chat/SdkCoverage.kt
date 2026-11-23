package com.sceyt.chat

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

private const val JACOCO_VERSION = "0.8.11"

private val coverageExcludes = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
)

/** Configures debug JVM coverage and reports for the SDK and demo modules. */
public fun Project.configureCoverage(sdkProjects: List<Project>, demoProjects: List<Project>) {
    extensions.configure<JacocoPluginExtension> {
        toolVersion = JACOCO_VERSION
    }

    val coveredProjects = sdkProjects + demoProjects
    coveredProjects.forEach { coveredProject ->
        coveredProject.pluginManager.apply("jacoco")
        coveredProject.extensions.configure<JacocoPluginExtension> {
            toolVersion = JACOCO_VERSION
        }
        coveredProject.tasks.withType(Test::class.java).configureEach {
            extensions.configure<JacocoTaskExtension> {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }

        coveredProject.registerCoverageReport(
            "jacocoDebugReport",
            "Generates debug unit-test coverage for ${coveredProject.path}.",
            listOf(coveredProject),
            "debug",
        )
    }

    registerCoverageReport(
        "jacocoSdkReport",
        "Generates aggregate unit-test coverage for all SDK library modules.",
        sdkProjects,
        "sdk",
    )
    registerCoverageReport(
        "jacocoProjectReport",
        "Generates aggregate unit-test coverage for every Android project module.",
        coveredProjects,
        "project",
    )
}

private fun Project.registerCoverageReport(
    taskName: String,
    taskDescription: String,
    coveredProjects: List<Project>,
    reportDirectory: String,
) {
    val output = layout.buildDirectory.dir("reports/jacoco/$reportDirectory")
    tasks.register<JacocoReport>(taskName) {
        group = "verification"
        description = taskDescription
        dependsOn(coveredProjects.map { "${it.path}:testDebugUnitTest" })

        sourceDirectories.setFrom(coveredProjects.map { it.debugCoverageSources() })
        classDirectories.setFrom(coveredProjects.map { it.debugCoverageClasses() })
        executionData.setFrom(coveredProjects.map { it.debugCoverageExecutionData() })

        reports {
            html.required.set(true)
            html.outputLocation.set(output.map { it.dir("html") })
            xml.required.set(true)
            xml.outputLocation.set(output.map { it.file("report.xml") })
            csv.required.set(true)
            csv.outputLocation.set(output.map { it.file("report.csv") })
        }
    }
}

private fun Project.debugCoverageClasses() = files(
    fileTree(
        tasks.named<KotlinJvmCompile>("compileDebugKotlin").flatMap { it.destinationDirectory }
    ) {
        exclude(coverageExcludes)
    },
    fileTree(
        layout.buildDirectory.dir(
            "intermediates/javac/debug/compileDebugJavaWithJavac/classes"
        )
    ) {
        exclude(coverageExcludes)
    },
)

private fun Project.debugCoverageSources() = files(
    "src/main/java",
    "src/main/kotlin",
)

private fun Project.debugCoverageExecutionData() = fileTree(layout.buildDirectory) {
    include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
}
