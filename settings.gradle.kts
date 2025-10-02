pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/")
        maven(url = "https://jcenter.bintray.com")
        maven(url = "https://jitpack.io")
        mavenLocal()
    }
}

rootProject.name = "SceytUiKit"

include(":SceytChatUiKit")
include(":SceytDemoApp")
project(":SceytDemoApp").projectDir = file("./examples/SceytDemoApp")
