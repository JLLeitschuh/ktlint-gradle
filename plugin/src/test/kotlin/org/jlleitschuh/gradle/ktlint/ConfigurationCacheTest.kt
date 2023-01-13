package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName

@GradleTestVersions
class ConfigurationCacheTest : AbstractPluginTest() {
    private val configurationCacheFlag = "--configuration-cache"
    private val configurationCacheWarnFlag = "--configuration-cache-problems=warn"

    @DisplayName("Should support configuration cache without errors on running linting")
    @CommonTest
    internal fun configurationCacheForCheckTask(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            createSourceFile(
                "src/main/kotlin/CleanSource.kt",
                """
                val foo = "bar"

                """.trimIndent()
            )

            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                CHECK_PARENT_TASK_NAME
            ) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                CHECK_PARENT_TASK_NAME
            ) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
                assertThat(output).contains("Reusing configuration cache.")
            }
        }
    }

    @DisplayName("Should support configuration cache on running format tasks")
    @CommonTest
    fun configurationCacheForFormatTasks(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            createSourceFile(
                "src/main/kotlin/CleanSource.kt",
                """
                val foo = "bar"
                """.trimIndent()
            )
            val formatTaskName = KtLintFormatTask.buildTaskNameForSourceSet("main")

            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                FORMAT_PARENT_TASK_NAME
            ) {
                assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(task(":$mainSourceSetFormatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                FORMAT_PARENT_TASK_NAME
            ) {
                assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(task(":$mainSourceSetFormatTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
                assertThat(output).contains("Reusing configuration cache.")
            }
        }
    }

    @DisplayName("Should support configuration cache for git hook format install task")
    @CommonTest
    internal fun configurationCacheForGitHookFormatInstallTask(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            projectPath.initGit()

            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                INSTALL_GIT_HOOK_FORMAT_TASK
            ) {
                assertThat(task(":$INSTALL_GIT_HOOK_FORMAT_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                INSTALL_GIT_HOOK_FORMAT_TASK
            ) {
                assertThat(task(":$INSTALL_GIT_HOOK_FORMAT_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(output).contains("Reusing configuration cache.")
            }
        }
    }

    @DisplayName("Should support configuration cache for git hook check install task")
    @CommonTest
    internal fun configurationCacheForGitHookCheckInstallTask(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            projectPath.initGit()

            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                INSTALL_GIT_HOOK_CHECK_TASK
            ) {
                assertThat(task(":$INSTALL_GIT_HOOK_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                INSTALL_GIT_HOOK_CHECK_TASK
            ) {
                assertThat(task(":$INSTALL_GIT_HOOK_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(output).contains("Reusing configuration cache.")
            }
        }
    }
}
