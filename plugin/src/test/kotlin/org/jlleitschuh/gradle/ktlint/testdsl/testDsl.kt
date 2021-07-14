package org.jlleitschuh.gradle.ktlint.testdsl

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.AbstractPluginTest
import java.io.File

fun AbstractPluginTest.project(
    gradleVersion: GradleVersion,
    projectPath: File = projectRoot,
    projectSetup: (File) -> Unit = defaultProjectSetup,
    test: TestProject.() -> Unit = {}
): TestProject {
    projectSetup(projectPath)

    val gradleRunner = GradleRunner.create()
        .withGradleVersion(gradleVersion.version)
        .withTestKitDir(sharedTestKitDir)
        .forwardOutput()
        .withProjectDir(projectPath)

    val testProject = TestProject(
        gradleRunner,
        gradleVersion,
        projectPath
    )

    testProject.test()
    return testProject
}

class TestProject(
    val gradleRunner: GradleRunner,
    val gradleVersion: GradleVersion,
    val projectPath: File
) {
    val buildGradle get() = projectPath.resolve("build.gradle")
    val settingsGradle get() = projectPath.resolve("settings.gradle")

    fun withCleanSources() {
        createSourceFile(
            CLEAN_SOURCES_FILE,
            """
            val foo = "bar"
            
            """.trimIndent()
        )
    }

    fun withFailingSources() {
        createSourceFile(
            FAIL_SOURCE_FILE,
            """
            val  foo    =     "bar"
            
            """.trimIndent()
        )
    }

    fun withCleanKotlinScript() {
        createSourceFile(
            "kotlin-script.kts",
            """
            println("zzz")
            
            """.trimIndent()
        )
    }

    fun withFailingKotlinScript() {
        createSourceFile(
            "kotlin-script-fail.kts",
            """
            println("zzz") 
            
            """.trimIndent()
        )
    }

    fun createSourceFile(
        sourceFilePath: String,
        contents: String
    ) {
        val sourceFile = projectPath.resolve(sourceFilePath)
        sourceFile.parentFile.mkdirs()
        sourceFile.writeText(contents)
    }

    fun restoreFailingSources() {
        val sourceFile = projectPath.resolve(FAIL_SOURCE_FILE)
        sourceFile.delete()
        withFailingSources()
    }

    fun removeSourceFile(sourceFilePath: String) {
        val sourceFile = projectPath.resolve(sourceFilePath)
        sourceFile.delete()
    }

    companion object {
        const val CLEAN_SOURCES_FILE = "src/main/kotlin/clean-source.kt"
        const val FAIL_SOURCE_FILE = "src/main/kotlin/fail-source.kt"
    }
}

fun TestProject.build(
    vararg buildArguments: String,
    assertions: BuildResult.() -> Unit = {}
) {
    gradleRunner
        .withArguments(buildArguments.toList() + "--stacktrace")
        .build()
        .run { assertions() }
}

fun TestProject.buildAndFail(
    vararg buildArguments: String,
    assertions: BuildResult.() -> Unit = {}
) {
    gradleRunner
        .withArguments(buildArguments.toList() + "--stacktrace")
        .buildAndFail()
        .run { assertions() }
}

val defaultProjectSetup: (File) -> Unit = projectSetup("jvm", "1.4.32")

fun projectSetup(
    kotlinPluginType: String,
    kotlinPluginVersion: String
): (File) -> Unit = {
    //language=Groovy
    it.resolve("build.gradle").writeText(
        """
        plugins {
            id 'org.jetbrains.kotlin.$kotlinPluginType'
            id 'org.jlleitschuh.gradle.ktlint'
        }
        
        repositories {
            mavenCentral()
        }
        
        """.trimIndent()
    )

    //language=Groovy
    it.resolve("settings.gradle").writeText(
        """
        pluginManagement {
            repositories {
                mavenLocal()
                gradlePluginPortal()
            }
            
            plugins {
                 id 'org.jetbrains.kotlin.$kotlinPluginType' version '$kotlinPluginVersion'
                 id 'org.jlleitschuh.gradle.ktlint' version '10.2.0-SNAPSHOT'
            }
        }
        
        """.trimIndent()
    )
}

private val sharedTestKitDir = File(".")
    .resolve(".gradle-test-kit")
    .absoluteFile
    .also {
        if (!it.exists()) it.mkdir()
    }
