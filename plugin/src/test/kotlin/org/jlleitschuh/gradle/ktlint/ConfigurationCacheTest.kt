package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.jlleitschuh.gradle.ktlint.testdsl.*
import org.junit.jupiter.api.DisplayName

@GradleTestVersions
class ConfigurationCacheTest : AbstractPluginTest() {
    private val configurationCacheFlag = "--configuration-cache"
    private val configurationCacheWarnFlag = "--configuration-cache-problems=warn"

    @DisplayName("Should support configuration cache without errors on running linting")
    @CommonTest
    internal fun configurationCacheForCheckTask(gradleVersion: GradleVersion) {
        project(gradleVersion = gradleVersion,
            projectSetup= ktsProjectSetup(gradleVersion)) {
            withFailingSources()
            withCleanSources()
            this.projectPath.resolve("build.gradle.kts").appendText(
                """
                |ktlint {
                |    filter { exclude("**/FailSource.kt") }
                |}
                |
                """.trimMargin()
            )
            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                CHECK_PARENT_TASK_NAME,
                "--info"
            ) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                CHECK_PARENT_TASK_NAME,
                "--info"
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
            withFailingSources()
            withCleanSources()
            val formatTaskName = KtLintFormatTask.buildTaskNameForSourceSet("main")
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.filter { exclude("**/FailSource.kt") }
                """.trimIndent()
            )
            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                FORMAT_PARENT_TASK_NAME,
                "--info"
            ) {
                assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(task(":$mainSourceSetFormatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                FORMAT_PARENT_TASK_NAME,
                "--info"
            ) {
                assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
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
