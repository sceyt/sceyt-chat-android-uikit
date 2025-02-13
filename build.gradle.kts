// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0" apply false
    id("com.google.firebase.crashlytics") version "3.0.3" apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

apply(plugin = "io.github.gradle-nexus.publish-plugin")
apply(from = "${rootDir}/maven-publish/publish-root.gradle")
