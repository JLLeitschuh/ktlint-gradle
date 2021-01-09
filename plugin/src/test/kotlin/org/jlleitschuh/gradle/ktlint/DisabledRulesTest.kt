package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jlleitschuh.gradle.ktlint.KtlintBasePlugin.Companion.LOWEST_SUPPORTED_GRADLE_VERSION
import org.jlleitschuh.gradle.ktlint.tasks.LoadRuleSetsTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Runs [EditorConfigTests] with the current version of Gradle.
 */
class GradleCurrentCurrentDisabledRulesTest : DisabledRulesTest()

/**
 * Runs [EditorConfigTests] with lowest supported Gradle version.
 */
@Suppress("ClassName")
class GradleLowestSupportedDisabledRulesTest : DisabledRulesTest() {
    override fun gradleRunnerFor(
        vararg arguments: String,
        projectRoot: File
    ): GradleRunner =
        super.gradleRunnerFor(*arguments, projectRoot = projectRoot)
            .withGradleVersion(LOWEST_SUPPORTED_GRADLE_VERSION)
}

abstract class DisabledRulesTest : AbstractPluginTest() {
    @BeforeEach
    internal fun setUp() {
        projectRoot.defaultProjectSetup()
    }

    @Test
    internal fun `Should lint without errors when 'final-newline' is disabled`() {
        projectRoot.buildFile().appendText(
            """

            ktlint.disabledRules = ["final-newline"]
            """.trimIndent()
        )

        projectRoot.createSourceFile(
            "src/main/kotlin/clean-source.kt",
            """
            val foo = "bar"
            """.trimIndent()
        )

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Should lint without errors when 'final-newline' and 'no-consecutive-blank-lines' are disabled`() {
        projectRoot.buildFile().appendText(
            """

            ktlint.disabledRules = ["final-newline", "no-consecutive-blank-lines"]
            """.trimIndent()
        )

        projectRoot.createSourceFile(
            "src/main/kotlin/clean-source.kt",
            """
            fun some() {


                print("Woohoo!")
            }
            
            val foo = "bar"
            """.trimIndent()
        )

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Should lint without errors when 'no-consecutive-blank-lines' are disable in the code`() {
        projectRoot.createSourceFile(
            "src/main/kotlin/clean-source.kt",
            """
            /* ktlint-disable no-consecutive-blank-lines */
            fun some() {


                print("Woohoo!")
            }
            /* ktlint-enable no-consecutive-blank-lines */
            
            val foo = "bar"
            
            """.trimIndent()
        )

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Should fail if ktLint version is lower then 0_34_2 and disabled rules configuration is set`() {
        projectRoot.buildFile().appendText(
            """

            ktlint.version = "0.34.0"
            ktlint.disabledRules = ["final-newline"]
            """.trimIndent()
        )

        projectRoot.withCleanSources()

        buildAndFail(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":${LoadRuleSetsTask.TASK_NAME}")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains("Rules disabling is supported since 0.34.2 ktlint version.")
        }
    }
}
