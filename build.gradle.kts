import com.diffplug.spotless.LineEnding
import com.sceyt.chat.configureCoverage
import com.sceyt.chat.configureSdkAbi

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    jacoco
    alias(libs.plugins.spotless)
    alias(libs.plugins.benchmark) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.compose) apply false
}

spotless {
    isEnforceCheck = false
    setLineEndings(LineEnding.UNIX)
    providers.gradleProperty("spotlessBase").orNull?.let { ratchetFrom(it) }

    val ktlintVersion = "1.8.0"
    // Start with the formatting rules defined by the repository's EditorConfig.
    val ktlintRules = mapOf(
        "ktlint_standard" to "disabled",
        "ktlint_standard_indent" to "enabled",
        "ktlint_standard_final-newline" to "enabled",
    )

    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(ktlintVersion).editorConfigOverride(ktlintRules)
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(ktlintVersion).editorConfigOverride(ktlintRules)
    }
}

val sdkProjects = listOf(project(":SceytChatUiKit"), project(":SceytChatConnection"))

sdkProjects.forEach { sdkProject ->
    sdkProject.plugins.withId("com.android.library") {
        sdkProject.configureSdkAbi()
    }
}

tasks.register("checkSdk") {
    group = "verification"
    description = "Runs Android lint and checks the release ABI for every SDK module."
    dependsOn(sdkProjects.flatMap { listOf("${it.path}:lintDebug", "${it.path}:checkSdkAbi") })
}

configureCoverage(sdkProjects, demoProjects = listOf(project(":SceytDemoApp")))
