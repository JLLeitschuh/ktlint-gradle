package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jlleitschuh.gradle.ktlint.KtlintBasePlugin.Companion.LOWEST_SUPPORTED_GRADLE_VERSION
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Runs [KotlinJsPluginTests] with the current version of Gradle.
 */
class GradleCurrentKotlinJsPluginTests : KotlinJsPluginTests()

/**
 * Runs [KotlinJsPluginTests] with lowest supported Gradle version.
 */
@Suppress("ClassName")
class GradleLowestSupportedKotlinJsPluginTest : KotlinJsPluginTests() {

    override fun gradleRunnerFor(
        vararg arguments: String,
        projectRoot: File
    ): GradleRunner =
        super.gradleRunnerFor(*arguments, projectRoot = projectRoot)
            .withGradleVersion(LOWEST_SUPPORTED_GRADLE_VERSION)
}

/**
 * Contains all tests related to "org.jetbrains.kotlin.js" plugin support.
 */
abstract class KotlinJsPluginTests : AbstractPluginTest() {
    @BeforeEach
    internal fun setUp() {
        projectRoot.kotlinPluginProjectSetup("org.jetbrains.kotlin.js")

        projectRoot.buildFile().appendText(
            """
                
                kotlin {
                    js {
                        nodejs()
                    }
                }
            """.trimMargin()
        )
    }

    @Test
    internal fun `Should add check tasks`() {
        build("-m", CHECK_PARENT_TASK_NAME).apply {
            val ktlintTasks = output
                .lineSequence()
                .toList()

            assertThat(ktlintTasks).anySatisfy {
                assertThat(it).contains(mainSourceSetCheckTaskName)
            }
            assertThat(ktlintTasks).anySatisfy {
                assertThat(it).contains(
                    GenerateReportsTask.generateNameForSourceSets(
                        "test",
                        GenerateReportsTask.LintType.CHECK
                    )
                )
            }
        }
    }

    @Test
    internal fun `Should add format tasks`() {
        build("-m", FORMAT_PARENT_TASK_NAME).apply {
            val ktlintTasks = output
                .lineSequence()
                .toList()

            assertThat(ktlintTasks).anySatisfy {
                assertThat(it).contains(mainSourceSetFormatTaskName)
            }
            assertThat(ktlintTasks).anySatisfy {
                assertThat(it).contains(
                    GenerateReportsTask.generateNameForSourceSets(
                        "test",
                        GenerateReportsTask.LintType.FORMAT
                    )
                )
            }
        }
    }

    @Test
    internal fun `Should fail check task on un-formatted sources`() {
        projectRoot.withFailingSources()

        buildAndFail(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
        }
    }
}
