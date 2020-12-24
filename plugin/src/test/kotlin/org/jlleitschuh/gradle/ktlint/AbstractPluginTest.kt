package org.jlleitschuh.gradle.ktlint

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File

abstract class AbstractPluginTest {

    @TempDir
    lateinit var temporaryFolder: File

    val projectRoot: File
        get() = temporaryFolder.resolve("plugin-test").apply { mkdirs() }

    protected fun pluginsBlockWithIdeaPlugin() =
        """
            plugins {
                id("org.jlleitschuh.gradle.ktlint-idea")
            }
        """.trimIndent()

    protected fun pluginsBlockWithMainPluginAndKotlinJvm() =
        """
            plugins {
                id 'org.jetbrains.kotlin.jvm'
                id 'org.jlleitschuh.gradle.ktlint'
            }
        """.trimIndent()

    protected fun build(
        vararg arguments: String,
        projectRoot: File = this.projectRoot
    ): BuildResult = gradleRunnerFor(*arguments, projectRoot = projectRoot).forwardOutput().build()

    protected
    fun buildAndFail(vararg arguments: String): BuildResult =
        gradleRunnerFor(arguments = arguments).forwardOutput().buildAndFail()

    protected open fun gradleRunnerFor(
        vararg arguments: String,
        projectRoot: File = this.projectRoot
    ): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectRoot)
            .withPluginClasspath()
            .withArguments(arguments.toList() + "--stacktrace")

    protected
    fun File.withCleanSources() = createSourceFile(
        "src/main/kotlin/clean-source.kt",
        """
            val foo = "bar"
            
        """.trimIndent()
    )

    protected
    fun File.withFailingSources() = createSourceFile(
        FAIL_SOURCE_FILE,
        """
            val  foo    =     "bar"
            
        """.trimIndent()
    )

    protected fun File.withCleanKotlinScript() = createSourceFile(
        "kotlin-script.kts",
        """
            println("zzz")
            
        """.trimIndent()
    )

    protected fun File.withFailingKotlinScript() = createSourceFile(
        "kotlin-script-fail.kts",
        """
            println("zzz") 
            
        """.trimIndent()
    )

    protected fun File.restoreFailingSources() {
        val sourceFile = resolve(FAIL_SOURCE_FILE)
        sourceFile.delete()
        withFailingSources()
    }

    protected
    fun File.withAlternativeFailingSources(baseDir: String) =
        createSourceFile("$baseDir/fail-source.kt", """val  foo    =     "bar"""")

    protected
    fun File.createSourceFile(sourceFilePath: String, contents: String) {
        val sourceFile = resolve(sourceFilePath)
        sourceFile.parentFile.mkdirs()
        sourceFile.writeText(contents)
    }

    protected
    fun File.removeSourceFile(sourceFilePath: String) {
        val sourceFile = resolve(sourceFilePath)
        sourceFile.delete()
    }

    fun File.settingsFile() = resolve("settings.gradle")

    companion object {
        const val FAIL_SOURCE_FILE = "src/main/kotlin/fail-source.kt"
    }
}
