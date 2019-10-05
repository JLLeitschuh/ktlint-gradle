package org.jlleitschuh.gradle.ktlint

import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GitHookTasksTest : AbstractPluginTest() {
    @BeforeEach
    internal fun setUp() {
        //language=Groovy
        projectRoot.buildFile().writeText("""
            ${pluginsBlockWithMainPluginAndKotlinJvm()}

            repositories {
                gradlePluginPortal()
            }
        """.trimIndent())
    }

    @Test
    internal fun `Should not add install git hook tasks to submodule`() {
        val submoduleDir = projectRoot.resolve("some-module").also { it.mkdir() }
        projectRoot.createGitHookFolder()
        projectRoot.settingsFile().writeText("""
            include ":some-module"
        """.trimIndent())
        //language=Groovy
        submoduleDir.buildFile().writeText("""
            apply plugin: "kotlin"
            apply plugin: "org.jlleitschuh.gradle.ktlint"
        """.trimIndent())

        buildAndFail(":some-module:$INSTALL_GIT_HOOK_CHECK_TASK").run {
            assertThat(output).contains("Task '$INSTALL_GIT_HOOK_CHECK_TASK' not found in project")
        }
        buildAndFail(":some-module:$INSTALL_GIT_HOOK_FORMAT_TASK").run {
            assertThat(output).contains("Task '$INSTALL_GIT_HOOK_FORMAT_TASK' not found in project")
        }
    }

    @Test
    internal fun `Should not add install git hook tasks to root project if git folder is not exist`() {
        buildAndFail(":$INSTALL_GIT_HOOK_CHECK_TASK").run {
            assertThat(output).contains("Task '$INSTALL_GIT_HOOK_CHECK_TASK' not found in root project")
        }
        buildAndFail(":$INSTALL_GIT_HOOK_FORMAT_TASK").run {
            assertThat(output).contains("Task '$INSTALL_GIT_HOOK_FORMAT_TASK' not found in root project")
        }
    }

    @Test
    internal fun `Running install git hook check task should create pre-commit hook`() {
        projectRoot.createGitHookFolder()

        build(":$INSTALL_GIT_HOOK_CHECK_TASK").run {
            assertThat(task(":$INSTALL_GIT_HOOK_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(projectRoot.preCommitGitHook()).exists()
            assertThat(projectRoot.preCommitGitHook().canExecute()).isTrue()
            assertThat(projectRoot.preCommitGitHook().readText()).contains(CHECK_PARENT_TASK_NAME)
        }
    }

    @Test
    internal fun `Running install git hook format task should create pre-commit hook`() {
        projectRoot.createGitHookFolder()

        build(":$INSTALL_GIT_HOOK_FORMAT_TASK").run {
            assertThat(task(":$INSTALL_GIT_HOOK_FORMAT_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(projectRoot.preCommitGitHook()).exists()
            assertThat(projectRoot.preCommitGitHook().canExecute()).isTrue()
            assertThat(projectRoot.preCommitGitHook().readText()).contains(FORMAT_PARENT_TASK_NAME)
        }
    }

    @Test
    internal fun `Should produce same hook on second run`() {
        projectRoot.createGitHookFolder()

        build(":$INSTALL_GIT_HOOK_FORMAT_TASK")
        val hookFileContent = projectRoot.preCommitGitHook().readText()
        build(":$INSTALL_GIT_HOOK_FORMAT_TASK")
        assertThat(projectRoot.preCommitGitHook().readText()).isEqualTo(hookFileContent)
    }

    @Test
    internal fun `Should not touch already existing hooks`() {
        projectRoot.createGitHookFolder()
        projectRoot.preCommitGitHook().writeText("""
            $shShebang

            echo "test1"
            $startHookSection


            $endHookSection
            echo "test2"
        """.trimIndent())

        build(":$INSTALL_GIT_HOOK_FORMAT_TASK")

        val hookContent = projectRoot.preCommitGitHook().readText()

        assertThat(hookContent).startsWith("""
            $shShebang

            echo "test1"
        """.trimIndent())
        assertThat(hookContent).endsWith("echo \"test2\"")
    }

    private fun File.preCommitGitHook(): File = gitHookFolder().resolve("pre-commit")
    private fun File.gitHookFolder(): File = resolve(".git/hooks/")
    private fun File.createGitHookFolder() = gitHookFolder().mkdirs()
}
