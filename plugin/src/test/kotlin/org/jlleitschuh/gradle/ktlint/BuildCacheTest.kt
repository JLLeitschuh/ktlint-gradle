package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jlleitschuh.gradle.ktlint.KtlintBasePlugin.Companion.LOWEST_SUPPORTED_GRADLE_VERSION
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
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
class GradleLowestSupportedBuildCacheTest : BuildCacheTest() {
    override fun gradleRunnerFor(
        vararg arguments: String,
        projectRoot: File
    ): GradleRunner =
        super.gradleRunnerFor(*arguments, projectRoot = projectRoot)
            .withGradleVersion(LOWEST_SUPPORTED_GRADLE_VERSION)
}

abstract class BuildCacheTest : AbstractPluginTest() {
    private val originalRoot get() = temporaryFolder.resolve("original").apply { mkdirs() }
    private val relocatedRoot get() = temporaryFolder.resolve("relocated").apply { mkdirs() }
    private val localBuildCache get() = temporaryFolder.resolve("build-cache").apply { mkdirs() }

    @Test
    fun `check task is relocatable`() {
        configureBuildCache()
        configureDefaultProjects()
        val testSourceCheckTaskName = GenerateReportsTask.generateNameForSourceSets(
            "test",
            GenerateReportsTask.LintType.CHECK
        )

        createRunner(originalRoot)
            .build().apply {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(task(":$testSourceCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

        createRunner(relocatedRoot)
            .build().apply {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
                assertThat(task(":$testSourceCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
            }
    }

    @Test
    internal fun `Check task with reporters is relocatable`() {
        configureBuildCache()
        configureDefaultProjects(
            """

            ktlint.reporters {
                reporter "plain"
                reporter "checkstyle"
                customReporters {
                    "html" {
                        fileExtension = "html"
                        dependency = "me.cassiano:ktlint-html-reporter:0.2.3"
                    }
                }
            }
            """.trimIndent()
        )

        createRunner(originalRoot)
            .build().apply {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

        createRunner(relocatedRoot)
            .build().apply {
                assertThat(task(":$mainSourceSetCheckTaskName")!!.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
            }
    }

    private fun createRunner(
        projectDir: File,
        taskToExecute: String = CHECK_PARENT_TASK_NAME
    ) = GradleRunner
        .create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(taskToExecute, "--build-cache", "-Dorg.gradle.caching.debug=true")
        .forwardOutput()

    private fun File.addBuildCacheSettings() = settingsFile()
        .writeText(
            """
            buildCache {
                local {
                    directory = '${localBuildCache.toURI()}'
                }
            }
            """.trimIndent()
        )

    private fun configureBuildCache() {
        originalRoot.addBuildCacheSettings()
        relocatedRoot.addBuildCacheSettings()
    }

    private fun configureDefaultProjects(
        additionalConfig: String = ""
    ) {
        listOf(originalRoot, relocatedRoot).forEach {
            it.withCleanSources()
            it.createSourceFile(
                "src/test/kotlin/Test.kt",
                """
                class Test
            
                """.trimIndent()
            )
            it.buildFile().writeText(
                """
                    ${pluginsBlockWithMainPluginAndKotlinJvm()}

                    repositories {
                        gradlePluginPortal()
                    }
                    
                    $additionalConfig

                """.trimIndent()
            )
        }
    }
}
