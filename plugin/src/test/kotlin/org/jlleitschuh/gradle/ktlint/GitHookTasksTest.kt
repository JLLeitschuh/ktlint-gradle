package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName
import java.io.File

@GradleTestVersions
class GitHookTasksTest : AbstractPluginTest() {

    @DisplayName("Should not add install git hook task to submodule")
    @CommonTest
    internal fun shouldNotAddInstallTaskToSubmodule(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val submoduleDir = projectPath.resolve("some-module").also { it.mkdirs() }
            projectPath.initGit()
            settingsGradle.appendText(
                """

                include ":some-module"
                """.trimIndent()
            )

            //language=Groovy
            submoduleDir.buildFile().writeText(
                """
                plugins {
                    id "org.jetbrains.kotlin.jvm"
                    id "org.jlleitschuh.gradle.ktlint"
                }
                """.trimIndent()
            )

            buildAndFail(":some-module:$INSTALL_GIT_HOOK_CHECK_TASK") {
                assertThat(output).contains("Task '$INSTALL_GIT_HOOK_CHECK_TASK' not found in project")
            }
            buildAndFail(":some-module:$INSTALL_GIT_HOOK_FORMAT_TASK") {
                assertThat(output).contains("Task '$INSTALL_GIT_HOOK_FORMAT_TASK' not found in project")
            }
        }
    }

    @DisplayName("Running install git hook check task should create pre-commit hook")
    @CommonTest
    fun installPreCommitHookCheck(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val gitDir = projectPath.initGit()

            build(":$INSTALL_GIT_HOOK_CHECK_TASK") {
                assertThat(task(":$INSTALL_GIT_HOOK_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(gitDir.preCommitGitHook()).exists()
                assertThat(gitDir.preCommitGitHook().canExecute()).isTrue
                assertThat(gitDir.preCommitGitHook().readText()).contains(CHECK_PARENT_TASK_NAME)
            }
        }
    }

    @DisplayName("Running install git hook format task should create pre-commit hook")
    @CommonTest
    fun installPreCommitHookFormat(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val gitDir = projectPath.initGit()

            build(":$INSTALL_GIT_HOOK_FORMAT_TASK") {
                assertThat(task(":$INSTALL_GIT_HOOK_FORMAT_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(gitDir.preCommitGitHook()).exists()
                assertThat(gitDir.preCommitGitHook().canExecute()).isTrue
                assertThat(gitDir.preCommitGitHook().readText()).contains(FORMAT_PARENT_TASK_NAME)
            }
        }
    }

    @DisplayName("Should produce same hook on second run")
    @CommonTest
    fun sameHookOnSecondRun(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val gitDir = projectPath.initGit()

            build(":$INSTALL_GIT_HOOK_FORMAT_TASK")
            val hookFileContent = gitDir.preCommitGitHook().readText()
            build(":$INSTALL_GIT_HOOK_FORMAT_TASK")
            assertThat(gitDir.preCommitGitHook().readText()).isEqualTo(hookFileContent)
        }
    }

    @DisplayName("Should not touch already existing hooks")
    @CommonTest
    fun notTouchExistingHooks(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val gitDir = projectPath.initGit()
            gitDir.preCommitGitHook().writeText(
                """
                $shShebang

                echo "test1"
                $startHookSection


                $endHookSection
                echo "test2"
                """.trimIndent()
            )

            build(":$INSTALL_GIT_HOOK_FORMAT_TASK")

            val hookContent = gitDir.preCommitGitHook().readText()

            assertThat(hookContent).startsWith(
                """
                $shShebang

                echo "test1"
                """.trimIndent()
            )
            assertThat(hookContent).endsWith("echo \"test2\"")
        }
    }

    @DisplayName("Should find git folder if Gradle project is not located in root git working dir")
    @CommonTest
    fun findGitDir(gradleVersion: GradleVersion) {
        val gradleRoot = projectRoot.resolve("internal/").also { it.mkdirs() }
        val gitDir = projectRoot.initGit()

        project(gradleVersion, projectPath = gradleRoot) {
            build(":$INSTALL_GIT_HOOK_CHECK_TASK") {
                assertThat(task(":$INSTALL_GIT_HOOK_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(gitDir.preCommitGitHook()).exists()
                assertThat(gitDir.preCommitGitHook().canExecute()).isTrue
                assertThat(gitDir.preCommitGitHook().readText()).contains(CHECK_PARENT_TASK_NAME)
            }
        }
    }

    @DisplayName("Check hook should not include files into git commit")
    @CommonTest
    fun checkHookShouldNotIncludeFilesIntoGitCommit(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val gitDir = projectPath.initGit()

            build(":$INSTALL_GIT_HOOK_CHECK_TASK") {
                assertThat(task(":$INSTALL_GIT_HOOK_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(gitDir.preCommitGitHook().readText()).doesNotContain("git add")
            }
        }
    }

    @DisplayName("Collects check run exit code and uses it to indicate check success")
    @CommonTest
    fun checkUsesGradleExitCode(gradleVersion: GradleVersion) {
        // This test ensures that we use the exit code of the check gradle command as the exit code
        // of the hook script to indicate success/failure instead of using set -e, because
        // that will prevent the saved un-staged changes from being re-applied to the working dir.
        // See [#551](https://github.com/JLLeitschuh/ktlint-gradle/pull/551)
        project(gradleVersion) {
            val gitDir = projectPath.initGit()

            build(":$INSTALL_GIT_HOOK_CHECK_TASK") {
                assertThat(task(":$INSTALL_GIT_HOOK_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(gitDir.preCommitGitHook().readText()).doesNotContain("set -e")
                assertThat(gitDir.preCommitGitHook().readText()).contains("gradleCommandExitCode=\$?")
                assertThat(gitDir.preCommitGitHook().readText()).contains("exit \$gradleCommandExitCode")
            }
        }
    }

    @DisplayName("Collects format run exit code and uses it to indicate format success")
    @CommonTest
    fun formatUsesGradleExitCode(gradleVersion: GradleVersion) {
        // This test ensures that we use the exit code of the format gradle command as the exit code
        // of the hook script to indicate success/failure instead of using set -e, because
        // that will prevent the saved un-staged changes from being re-applied to the working dir.
        // See [#551](https://github.com/JLLeitschuh/ktlint-gradle/pull/551)
        project(gradleVersion) {
            val gitDir = projectPath.initGit()

            build(":$INSTALL_GIT_HOOK_FORMAT_TASK") {
                assertThat(task(":$INSTALL_GIT_HOOK_FORMAT_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(gitDir.preCommitGitHook().readText()).doesNotContain("set -e")
                assertThat(gitDir.preCommitGitHook().readText()).contains("gradleCommandExitCode=\$?")
                assertThat(gitDir.preCommitGitHook().readText()).contains("exit \$gradleCommandExitCode")
            }
        }
    }

    @DisplayName("Format hook should include updated files into git commit")
    @CommonTest
    fun formatIncludeUpdatedFiles(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val gitDir = projectPath.initGit()

            build(":$INSTALL_GIT_HOOK_FORMAT_TASK") {
                assertThat(task(":$INSTALL_GIT_HOOK_FORMAT_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(gitDir.preCommitGitHook().readText()).contains("git add")
            }
        }
    }

    @DisplayName("Format hook should not add non-indexed code to the commit")
    @CommonTest
    fun formatHookNonIndexedCode(gradleVersion: GradleVersion) {
        // TODO: This test doesn't run git or verify that only indexed code is committed,
        //  only that the hook contains the correct commands.
        //  Ideally an end to end test case would do something like:
        //
        //  echo "val a  = 1" > test.kt
        //  git add test.kt
        //  echo "val b  = 2" >> test.kt
        //  git commit -m test
        //  assert that commit equals "val a = 1"
        //  assert that test.kt equals "val a = 1\nval b  = 2"

        project(gradleVersion) {
            val gitDir = projectPath.initGit()

            build(":$INSTALL_GIT_HOOK_FORMAT_TASK") {
                assertThat(task(":$INSTALL_GIT_HOOK_FORMAT_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                val hookText = gitDir.preCommitGitHook().readText()
                assertThat(hookText).contains("git diff --color=never > \$diff")
                assertThat(hookText).contains("git apply -R \$diff")
                assertThat(hookText).contains("git apply --ignore-whitespace \$diff")
                assertThat(hookText).contains("rm \$diff")
            }
        }
    }

    @DisplayName("Format hook should format files when they are renamed")
    @CommonTest
    fun formatHookRenamedFiles(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val gitDir = projectPath.initGit()

            build(":$INSTALL_GIT_HOOK_FORMAT_TASK") {
                assertThat(task(":$INSTALL_GIT_HOOK_FORMAT_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(gitDir.preCommitGitHook().readText()).contains("""{ print $NF }""")
            }
        }
    }

    @DisplayName("Format hook should only format files that end with .kt or .kts")
    @CommonTest
    fun formatHookFilterByExtension(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val gitDir = projectPath.initGit()

            build(":$INSTALL_GIT_HOOK_FORMAT_TASK") {
                assertThat(task(":$INSTALL_GIT_HOOK_FORMAT_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(gitDir.preCommitGitHook().readText()).contains("""/\.kts?$/""")
            }
        }
    }

    private fun File.preCommitGitHook(): File = gitHookFolder().resolve("pre-commit")

    private fun File.gitHookFolder(): File = resolve("hooks/")
}
