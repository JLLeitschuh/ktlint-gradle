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

@GradleTestVersions(minVersion = "6.6.1")
class ConfigurationCacheTest : AbstractPluginTest() {
    private val configurationCacheFlag = "--configuration-cache"
    private val configurationCacheWarnFlag = "--configuration-cache-problems=warn"

    /**
     * 2 warnings are still reported by the Kotlin plugin. We can't fix them.
     * But make sure we aren't creating more issues.
     * ```
     * 2 problems were found storing the configuration cache, 1 of which seems unique.
     * plugin 'org.jetbrains.kotlin.jvm': registration of listener on 'Gradle.addBuildListener' is unsupported
     * See https://docs.gradle.org/6.6-milestone-3/userguide/configuration_cache.html#config_cache:requirements:build_listeners
     * ```
     */
    private val maxProblemsFlag = "-Dorg.gradle.unsafe.configuration-cache.max-problems=2"

    @DisplayName("Should support configuration cache without errors on running linting")
    @CommonTest
    internal fun configurationCacheForCheckTask(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            createSourceFile(
                "src/main/kotlin/clean-source.kt",
                """
                val foo = "bar"
                
                """.trimIndent()
            )

            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                maxProblemsFlag,
                CHECK_PARENT_TASK_NAME
            ) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                maxProblemsFlag,
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
                "src/main/kotlin/clean-source.kt",
                """
                val foo = "bar"
                """.trimIndent()
            )
            val formatTaskName = KtLintFormatTask.buildTaskNameForSourceSet("main")

            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                maxProblemsFlag,
                FORMAT_PARENT_TASK_NAME
            ) {
                assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(task(":$mainSourceSetFormatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            build(
                configurationCacheFlag,
                configurationCacheWarnFlag,
                maxProblemsFlag,
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
