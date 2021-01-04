package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.lib.RepositoryBuilder
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File

class GitHookTasksTest : AbstractPluginTest() {
    @Test
    internal fun `Should not add install git hook tasks to submodule`() {
        projectRoot.setupGradleProject()
        val submoduleDir = projectRoot.resolve("some-module").also { it.mkdir() }
        projectRoot.initGit()
        projectRoot.settingsFile().writeText(
            """
            include ":some-module"
            """.trimIndent()
        )
        //language=Groovy
        submoduleDir.buildFile().writeText(
            """
            apply plugin: "kotlin"
            apply plugin: "org.jlleitschuh.gradle.ktlint"
            """.trimIndent()
        )

        buildAndFail(":some-module:$INSTALL_GIT_HOOK_CHECK_TASK").run {
            assertThat(output).contains("Task '$INSTALL_GIT_HOOK_CHECK_TASK' not found in project")
        }
        buildAndFail(":some-module:$INSTALL_GIT_HOOK_FORMAT_TASK").run {
            assertThat(output).contains("Task '$INSTALL_GIT_HOOK_FORMAT_TASK' not found in project")
        }
    }

    @Test
    internal fun `Running install git hook check task should create pre-commit hook`() {
        projectRoot.setupGradleProject()
        val gitDir = projectRoot.initGit()

        build(":$INSTALL_GIT_HOOK_CHECK_TASK").run {
            assertThat(task(":$INSTALL_GIT_HOOK_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(gitDir.preCommitGitHook()).exists()
            assertThat(gitDir.preCommitGitHook().canExecute()).isTrue
            assertThat(gitDir.preCommitGitHook().readText()).contains(CHECK_PARENT_TASK_NAME)
        }
    }

    @Test
    internal fun `Running install git hook format task should create pre-commit hook`() {
        projectRoot.setupGradleProject()
        val gitDir = projectRoot.initGit()

        build(":$INSTALL_GIT_HOOK_FORMAT_TASK").run {
            assertThat(task(":$INSTALL_GIT_HOOK_FORMAT_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(gitDir.preCommitGitHook()).exists()
            assertThat(gitDir.preCommitGitHook().canExecute()).isTrue()
            assertThat(gitDir.preCommitGitHook().readText()).contains(FORMAT_PARENT_TASK_NAME)
        }
    }

    @Test
    internal fun `Should produce same hook on second run`() {
        projectRoot.setupGradleProject()
        val gitDir = projectRoot.initGit()

        build(":$INSTALL_GIT_HOOK_FORMAT_TASK")
        val hookFileContent = gitDir.preCommitGitHook().readText()
        build(":$INSTALL_GIT_HOOK_FORMAT_TASK")
        assertThat(gitDir.preCommitGitHook().readText()).isEqualTo(hookFileContent)
    }

    @Test
    internal fun `Should not touch already existing hooks`() {
        projectRoot.setupGradleProject()
        val gitDir = projectRoot.initGit()
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

    @Test
    internal fun `Should find git folder if Gradle project is not located in root git working dir`() {
        val gradleRoot = projectRoot.resolve("internal/")
        gradleRoot.mkdir()
        gradleRoot.setupGradleProject()
        val gitDir = projectRoot.initGit()

        build(":$INSTALL_GIT_HOOK_CHECK_TASK", projectRoot = gradleRoot).run {
            assertThat(task(":$INSTALL_GIT_HOOK_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(gitDir.preCommitGitHook()).exists()
            assertThat(gitDir.preCommitGitHook().canExecute()).isTrue()
            assertThat(gitDir.preCommitGitHook().readText()).contains(CHECK_PARENT_TASK_NAME)
        }
    }

    @Test
    internal fun `Check hook should not include files into git commit`() {
        projectRoot.setupGradleProject()
        val gitDir = projectRoot.initGit()

        build(":$INSTALL_GIT_HOOK_CHECK_TASK").run {
            assertThat(task(":$INSTALL_GIT_HOOK_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(gitDir.preCommitGitHook().readText()).doesNotContain("git add")
        }
    }

    @Test
    internal fun `Format hook should include updated files into git commit`() {
        projectRoot.setupGradleProject()
        val gitDir = projectRoot.initGit()

        build(":$INSTALL_GIT_HOOK_FORMAT_TASK").run {
            assertThat(task(":$INSTALL_GIT_HOOK_FORMAT_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(gitDir.preCommitGitHook().readText()).contains("git add")
        }
    }

    private fun File.initGit(): File {
        val repo = RepositoryBuilder().setWorkTree(this).setMustExist(false).build()
        repo.create()
        return repo.directory
    }

    private fun File.preCommitGitHook(): File = gitHookFolder().resolve("pre-commit")

    private fun File.gitHookFolder(): File = resolve("hooks/")

    private fun File.setupGradleProject() {
        //language=Groovy
        buildFile().writeText(
            """
            ${pluginsBlockWithMainPluginAndKotlinJvm()}

            repositories {
                gradlePluginPortal()
            }
            """.trimIndent()
        )
    }
}
