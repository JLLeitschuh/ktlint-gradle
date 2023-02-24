package org.jlleitschuh.gradle.ktlint.testdsl

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.AbstractPluginTest
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
    val editorConfig get() = projectPath.resolve(".editorconfig")

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

    fun withAdditionalEditorConfig() {
        createSourceFile(
            ADDITIONAL_EDITOR_CONFIG + "/.editorconfig",
            """
            |[*.kt]
            |disabled_rules = no-multi-spaces
            |ktlint_disabled_rules = no-multi-spaces
            |ktlint_standard_no-multi-spaces = disabled
            """.trimMargin()
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
        const val ADDITIONAL_EDITOR_CONFIG = "AdditionalEditorConfig"
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

fun defaultProjectSetup(gradleVersion: GradleVersion): (File) -> Unit =
    projectSetup("jvm", gradleVersion)

private val GradleVersion.supportedKotlinVersion
    get() = TestVersions.maxSupportedKotlinPluginVersion(this)

fun projectSetup(
    kotlinPluginType: String,
    gradleVersion: GradleVersion,
): (File) -> Unit = {
    val kotlinPluginVersion = gradleVersion.supportedKotlinVersion
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
