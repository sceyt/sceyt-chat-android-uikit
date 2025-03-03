// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.nexus.publish) apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

apply(plugin = "io.github.gradle-nexus.publish-plugin")
apply(from = "${rootDir}/maven-publish/publish-root.gradle")
