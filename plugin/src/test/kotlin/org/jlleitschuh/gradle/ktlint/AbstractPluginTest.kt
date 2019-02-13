package org.jlleitschuh.gradle.ktlint

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.TextUtil.normaliseFileSeparators
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Properties

abstract class AbstractPluginTest {

    @TempDir
    lateinit var temporaryFolder: File

    val projectRoot: File
        get() = temporaryFolder.resolve("plugin-test").apply { mkdirs() }

    protected
    fun buildscriptBlockWithUnderTestPlugin() =
        """
            buildscript {
                repositories { maven { url = "$testRepositoryPath" } }
                dependencies {
                    classpath("org.jlleitschuh.gradle:ktlint-gradle:${testProperties["version"]}")
                }
            }
        """.trimIndent()

    protected
    fun pluginsBlockWithKotlinJvmPlugin() =
        """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "${testProperties["kotlinVersion"]}"
            }
        """.trimIndent()

    protected
    fun build(vararg arguments: String): BuildResult =
        gradleRunnerFor(*arguments).forwardOutput().build()

    protected
    fun buildAndFail(vararg arguments: String): BuildResult =
        gradleRunnerFor(*arguments).forwardOutput().buildAndFail()

    protected open
    fun gradleRunnerFor(vararg arguments: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectRoot)
            .withArguments(arguments.toList() + "--stacktrace")

    private
    val testRepositoryPath
        get() = normaliseFileSeparators(File("build/plugin-test-repository").absolutePath)

    protected
    val testProperties: Properties by lazy {
        javaClass.getResourceAsStream("/test.properties").use {
            Properties().apply { load(it) }
        }
    }

    protected
    fun File.withCleanSources() = createSourceFile("src/main/kotlin/clean-source.kt", """val foo = "bar"""")

    protected
    fun File.withFailingSources() = createSourceFile("src/main/kotlin/fail-source.kt", """val  foo    =     "bar"""")

    protected fun File.restoreFailingSources() {
        val sourceFile = resolve("src/main/kotlin/fail-source.kt")
        sourceFile.delete()
        withFailingSources()
    }

    protected
    fun File.withAlternativeFailingSources(baseDir: String) =
        createSourceFile("$baseDir/fail-source.kt", """val  foo    =     "bar"""")

    private
    fun File.createSourceFile(sourceFilePath: String, contents: String) {
        val sourceFile = resolve(sourceFilePath)
        sourceFile.parentFile.mkdirs()
        sourceFile.writeText(contents)
    }

    protected fun File.createEditorconfigFile(
        maxLineLength: Int = 120
    ) = createSourceFile(".editorconfig", """
        [*.{kt,kts}]
        max_line_length=$maxLineLength
    """.trimIndent())

    protected fun File.modifyEditorconfigFile(
        maxLineLength: Int
    ) {
        val editorconfigFile = resolve(".editorconfig")
        if (editorconfigFile.exists()) {
            editorconfigFile.delete()
        }
        createEditorconfigFile(maxLineLength)
    }

    fun File.buildFile() = resolve("build.gradle")
    fun File.settingsFile() = resolve("settings.gradle")
}
