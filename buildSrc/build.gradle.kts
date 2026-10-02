plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    //noinspection UseTomlInstead
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    implementation("com.android.tools.build:gradle:9.4.1")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.37.0")
}
