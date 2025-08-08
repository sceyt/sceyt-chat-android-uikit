plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
    implementation("com.android.tools.build:gradle:8.11.1")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.34.0")
}
