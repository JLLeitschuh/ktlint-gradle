package org.jlleitschuh.gradle.ktlint.testdsl

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.AbstractPluginTest
import org.junit.jupiter.api.condition.OS
import java.io.File

fun AbstractPluginTest.project(
    gradleVersion: GradleVersion,
    projectPath: File = projectRoot,
    projectSetup: (File) -> Unit = defaultProjectSetup(gradleVersion),
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

    fun withCleanKotlinScript() {
        //@formatter:off
        createSourceFile(
            "kotlin-script.kts",
            """
            |println("zzz")
            |
            """.trimMargin()
        )
        //@formatter:on
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
        const val CLEAN_SOURCES_FILE = "src/main/kotlin/clean-source.kt"
        const val FAIL_SOURCE_FILE = "src/main/kotlin/fail-source.kt"
    }
}

fun TestProject.build(
    vararg buildArguments: String,
    assertions: BuildResult.() -> Unit = {}
) {
    val daemonFlag = if (OS.WINDOWS.isCurrentOs) listOf("--no-daemon") else emptyList()
    gradleRunner
        .withArguments(buildArguments.toList() + "--stacktrace" + daemonFlag)
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

fun defaultProjectSetup(gradleVersion: GradleVersion): (File) -> Unit =
    projectSetup("jvm", gradleVersion)

private val GradleVersion.supportedKotlinVersion
    get() = if (this <= GradleVersion.version("6.1.1")) {
        TestVersions.minSupportedKotlinPluginVersion
    } else {
        TestVersions.maxSupportedKotlinPluginVersion
    }

fun projectSetup(
    kotlinPluginType: String,
    gradleVersion: GradleVersion,
): (File) -> Unit = {
    val kotlinPluginVersion = gradleVersion.supportedKotlinVersion
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
         id 'org.jlleitschuh.gradle.ktlint' version '${TestVersions.pluginVersion}'
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
