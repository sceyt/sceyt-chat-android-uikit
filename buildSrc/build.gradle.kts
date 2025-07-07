plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
    implementation("com.android.tools.build:gradle:8.6.1")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.33.0")
}
