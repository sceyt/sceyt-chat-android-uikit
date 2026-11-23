package com.sceyt.chat

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.abi.AbiValidationToolchain.Companion.abiValidation
import org.jetbrains.kotlin.buildtools.api.abi.dumpJvmAbiToStringOperation
import java.net.URLClassLoader
import java.util.zip.ZipFile

/** Registers ABI validation for the Android library's release AAR. */
public fun Project.configureSdkAbi() {
    val reference = layout.projectDirectory.file("api/$name.api")
    val dump = tasks.register<DumpSdkAbi>("dumpSdkAbi") {
        buildToolsClasspath.from(configurations.named("kotlinBuildToolsApiClasspath"))
        apiFile.set(layout.buildDirectory.file("reports/abi/${project.name}.api"))
    }

    extensions.configure<LibraryAndroidComponentsExtension> {
        onVariants(selector().withBuildType("release")) { variant ->
            dump.configure { aarFile.set(variant.artifacts.get(SingleArtifact.AAR)) }
        }
    }

    val generated = dump.flatMap { it.apiFile }
    val updateTask = "${path}:updateSdkAbi"
    tasks.register("checkSdkAbi") {
        group = "verification"
        description = "Checks the SDK's release ABI against its reference file."
        mustRunAfter("updateSdkAbi")
        inputs.file(reference)
        inputs.file(generated)
        doLast {
            val actual = generated.get().asFile
            if (reference.asFile.readLines() != actual.readLines()) {
                throw GradleException(
                    "SDK ABI differs. Compare ${reference.asFile} with $actual. " +
                        "Run $updateTask to accept an intentional change."
                )
            }
        }
    }

    tasks.register<Copy>("updateSdkAbi") {
        group = "verification"
        description = "Updates the reference ABI for the SDK's release AAR."
        from(generated)
        into(reference.asFile.parentFile)
    }
}

@CacheableTask
internal abstract class DumpSdkAbi : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val aarFile: RegularFileProperty

    @get:Classpath
    abstract val buildToolsClasspath: ConfigurableFileCollection

    @get:OutputFile
    abstract val apiFile: RegularFileProperty

    @TaskAction
    @OptIn(ExperimentalBuildToolsApi::class)
    fun dump() {
        val archive = aarFile.get().asFile
        val classes = temporaryDir.resolve("classes.jar")
        ZipFile(archive).use { aar ->
            val entry = requireNotNull(aar.getEntry("classes.jar")) { "$archive has no classes.jar" }
            aar.getInputStream(entry).use { input ->
                classes.outputStream().use { input.copyTo(it) }
            }
        }

        val classpath = buildToolsClasspath.files.map { it.toURI().toURL() }.toTypedArray()
        URLClassLoader(classpath, KotlinToolchains::class.java.classLoader).use { loader ->
            val toolchains = KotlinToolchains.loadImplementation(loader)
            val abi = StringBuilder()
            val operation = toolchains.abiValidation.dumpJvmAbiToStringOperation(abi, listOf(classes.toPath()))
            toolchains.createBuildSession().use { it.executeOperation(operation) }
            apiFile.get().asFile.writeText(abi.toString().trimEnd('\n', '\r') + "\n")
        }
    }
}
