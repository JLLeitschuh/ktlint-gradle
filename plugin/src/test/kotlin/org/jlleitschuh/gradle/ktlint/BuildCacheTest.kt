package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Runs [BuildCacheTest] with the current version of Gradle.
 */
class GradleCurrentBuildCacheTest : BuildCacheTest()

/**
 * Runs [BuildCacheTest] with lowest supported Gradle version.
 */
@Suppress("ClassName")
class Gradle4_10BuildCacheTest : BuildCacheTest() {
    override fun gradleRunnerFor(vararg arguments: String): GradleRunner =
        super.gradleRunnerFor(*arguments).withGradleVersion("4.10")
}

abstract class BuildCacheTest : AbstractPluginTest() {
    private val originalRoot get() = temporaryFolder.resolve("original").apply { mkdirs() }
    private val relocatedRoot get() = temporaryFolder.resolve("relocated").apply { mkdirs() }
    private val localBuildCache get() = temporaryFolder.resolve("build-cache").apply { mkdirs() }

    @Test
    fun `check task is relocatable`() {
        configureBuildCache()
        configureDefaultProjects()

        createRunner(originalRoot)
            .build().apply {
                assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

        createRunner(relocatedRoot)
            .build().apply {
                assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
            }
    }

    @Test
    internal fun `Check task with reporters is relocatable`() {
        configureBuildCache()
        configureDefaultProjects("""
            import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

            ktlint.reporters = [ReporterType.PLAIN, ReporterType.CHECKSTYLE]
            ktlint.customReporters {
                reporter "html", "html", "me.cassiano:ktlint-html-reporter:0.2.3"
            }
        """.trimIndent())

        createRunner(originalRoot)
            .build().apply {
                assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

        createRunner(relocatedRoot)
            .build().apply {
                assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
            }
    }

    private fun createRunner(
        projectDir: File,
        taskToExecute: String = "ktlintCheck"
    ) = GradleRunner
        .create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(taskToExecute, "--build-cache")
        .forwardOutput()

    private fun File.addBuildCacheSettings() = settingsFile()
        .writeText("""
            buildCache {
                local {
                    directory = '${localBuildCache.toURI()}'
                }
            }
        """.trimIndent())

    private fun configureBuildCache() {
        originalRoot.addBuildCacheSettings()
        relocatedRoot.addBuildCacheSettings()
    }

    private fun configureDefaultProjects(
        additionalConfig: String = ""
    ) {
        listOf(originalRoot, relocatedRoot).forEach {
            it.withCleanSources()
            it.buildFile().writeText("""
                    ${pluginsBlockWithMainPluginAndKotlinJvm()}

                    repositories {
                        gradlePluginPortal()
                    }
                    
                    $additionalConfig

                """.trimIndent())
        }
    }
}