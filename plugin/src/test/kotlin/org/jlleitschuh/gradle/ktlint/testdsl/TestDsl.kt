package org.jlleitschuh.gradle.ktlint.testdsl

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.AbstractPluginTest
import org.jlleitschuh.gradle.ktlint.testdsl.TestVersions.maxSupportedKotlinPluginVersion
import java.io.File

fun AbstractPluginTest.project(
    gradleVersion: GradleVersion,
    projectPath: File = projectRoot,
    projectSetup: (File) -> Unit = defaultProjectSetup(),
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
    val editorConfig get() = projectPath.resolve(".editorconfig")

    fun withCleanSources(filePath: String = CLEAN_SOURCES_FILE) {
        createSourceFile(
            filePath,
            """
            |val foo = "bar"
            |
            """.trimMargin()
        )
    }

    fun withFailingSources() {
        createSourceFile(
            FAIL_SOURCE_FILE,
            """
            |val  foo    =     "bar"
            |
            """.trimMargin()
        )
    }

    fun withFailingMaxLineSources() {
        createSourceFile(
            FAIL_SOURCE_FILE,
            buildString {
                append("val nameOfVariable =")
                append("\n")
                append("    listOf(1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1, 2, 3, 2)")
                append("\n")
            }
        )
    }

    fun withCleanKotlinScript() {
        createSourceFile(
            "kotlin-script.kts",
            """
            |println("zzz")
            |
            """.trimMargin()
        )
    }

    fun withFailingKotlinScript() {
        createSourceFile(
            "kotlin-script-fail.kts",
            """
            |println("zzz")@
            |
            """.trimMargin()
                .replace('@', ' ')
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
        const val CLEAN_SOURCES_FILE = "src/main/kotlin/CleanSource.kt"
        const val FAIL_SOURCE_FILE = "src/main/kotlin/FailSource.kt"
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

fun defaultProjectSetup(): (File) -> Unit =
    projectSetup("jvm")

fun projectSetup(
    kotlinPluginType: String,
    kotlinPluginVersion: String = maxSupportedKotlinPluginVersion
): (File) -> Unit = {
    //language=Groovy
    it.resolve("build.gradle").writeText(
        """
        |plugins {
        |    id 'org.jetbrains.kotlin.$kotlinPluginType'
        |    id 'org.jlleitschuh.gradle.ktlint'
        |}
        |
        |repositories {
        |    mavenCentral()
        |}
        |
        """.trimMargin()
    )

    //language=Groovy
    it.resolve("settings.gradle").writeText(
        """
        |pluginManagement {
        |    repositories {
        |        mavenLocal()
        |        gradlePluginPortal()
        |    }
        |
        |    plugins {
        |         id 'org.jetbrains.kotlin.$kotlinPluginType' version '$kotlinPluginVersion'
        |         id 'org.jlleitschuh.gradle.ktlint' version '${TestVersions.pluginVersion}'
        |    }
        |}
        |
        """.trimMargin()
    )
}

private val sharedTestKitDir = File(".")
    .resolve(".gradle-test-kit")
    .absoluteFile
    .also {
        if (!it.exists()) it.mkdir()
    }
