package com.sceyt.chat

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

fun Project.configureMavenPublishing() {
    plugins.apply("com.vanniktech.maven.publish")
    plugins.apply("maven-publish")
    
    configure<MavenPublishBaseExtension> {
        coordinates(
            groupId = Config.mavenCentralGroup,
            artifactId = Config.mavenCentralArtifactId,
            version = Config.mavenCentralVersion
        )

        pom {
            name.set(Config.mavenCentralArtifactId)
            description.set("Sceyt Chat Android UIKit")
            url.set("https://github.com/sceyt/sceyt-chat-android-uikit")

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://github.com/sceyt/sceyt-chat-android-uikit/blob/master/LICENSE")
                }
            }

            developers {
                developer {
                    id.set("maratsct")
                    name.set("Marat Hambikyan")
                    email.set("marat@sceyt.com")
                }
            }

            scm {
                connection.set("scm:git:github.com/sceyt/sceyt-chat-android-uikit.git")
                developerConnection.set("scm:git:ssh://github.com/sceyt/sceyt-chat-android-uikit.git")
                url.set("https://github.com/sceyt/sceyt-chat-android-uikit/tree/master")
            }
        }

        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        signAllPublications()
    }
} 