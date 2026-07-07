plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    //noinspection UseTomlInstead
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
    implementation("com.android.tools.build:gradle:8.12.0")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.34.0")
}
