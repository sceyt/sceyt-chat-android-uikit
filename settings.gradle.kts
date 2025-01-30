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
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
        maven {
            url = uri("https://jcenter.bintray.com")
        }
        maven {
            url = uri("https://jitpack.io")
        }
        mavenLocal()
    }
}

rootProject.name = "SceytUiKit"

include(":SceytChatUiKit")
include(":SceytDemoApp")
project(":SceytDemoApp").projectDir = file("./examples/SceytDemoApp")
