package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.TestProject
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.getMajorJavaVersion
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DisplayName
import java.io.File

@GradleTestVersions
class BuildCacheTest : AbstractPluginTest() {
    private val originalRoot get() = temporaryFolder.resolve("original").apply { mkdirs() }
    private val relocatedRoot get() = temporaryFolder.resolve("relocated").apply { mkdirs() }
    private val localBuildCache get() = temporaryFolder.resolve("build-cache").apply { mkdirs() }

    @DisplayName("Check task should be relocatable")
    @CommonTest
    fun checkIsRelocatable(gradleVersion: GradleVersion) {
        val testSourceCheckTaskName = GenerateReportsTask.generateNameForSourceSets(
            "test",
            GenerateReportsTask.LintType.CHECK
        )
        project(gradleVersion, projectPath = originalRoot) {
            configureDefaultProject()

            build(CHECK_PARENT_TASK_NAME, "--build-cache") {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(task(":$testSourceCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }

        project(gradleVersion, projectPath = relocatedRoot) {
            configureDefaultProject()

            build(CHECK_PARENT_TASK_NAME, "--build-cache") {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
                assertThat(task(":$testSourceCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
            }
        }
    }

    @DisplayName("Check task with additional reporters should be relocatable")
    @CommonTest
    fun checkWithReportersIsRelocatable(gradleVersion: GradleVersion) {
        // custom reporter is compiled on java 11
        Assumptions.assumeTrue(getMajorJavaVersion() >= 11)
        project(gradleVersion, projectPath = originalRoot) {
            configureDefaultProject()
            useExternalKtLintReporter()

            build(CHECK_PARENT_TASK_NAME, "--build-cache") {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }

        project(gradleVersion, projectPath = relocatedRoot) {
            configureDefaultProject()
            useExternalKtLintReporter()

            build(CHECK_PARENT_TASK_NAME, "--build-cache") {
                assertThat(task(":$mainSourceSetCheckTaskName")!!.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
            }
        }
    }

    private fun TestProject.useExternalKtLintReporter() = buildGradle
        .appendText(
            //language=Groovy
            """
            ktlint.reporters {
                reporter "plain"
                reporter "checkstyle"
                customReporters {
                    "github" {
                        dependency = "de.musichin.ktlint.reporter:ktlint-reporter-github:3.1.0"
                    }
                }
            }
            """.trimIndent()
        )

    private fun File.addBuildCacheSettings() = appendText(
        //language=Groovy
        """

        buildCache {
            local {
                directory = '${localBuildCache.toURI()}'
            }
        }
        """.trimIndent()
    )

    private fun TestProject.configureDefaultProject() {
        settingsGradle.addBuildCacheSettings()

        withCleanSources()
        createSourceFile(
            "src/test/kotlin/Test.kt",
            """
                class Test

            """.trimIndent()
        )
    }
}
