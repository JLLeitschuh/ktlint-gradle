package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jlleitschuh.gradle.ktlint.KtlintBasePlugin.Companion.LOWEST_SUPPORTED_GRADLE_VERSION
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Runs the tests with the current version of Gradle.
 */
class GradleCurrentKtlintPluginTest : BaseKtlintPluginTest()

@Suppress("ClassName")
class GradleLowestSupportedKtlintPluginTest : BaseKtlintPluginTest() {

    override fun gradleRunnerFor(
        vararg arguments: String,
        projectRoot: File
    ): GradleRunner =
        super.gradleRunnerFor(*arguments, projectRoot = projectRoot)
            .withGradleVersion(LOWEST_SUPPORTED_GRADLE_VERSION)
}

abstract class BaseKtlintPluginTest : AbstractPluginTest() {

    @BeforeEach
    fun setupBuild() {
        projectRoot.defaultProjectSetup()
    }

    @Test
    fun `should fail check on failing sources`() {
        projectRoot.withFailingSources()

        buildAndFail(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains("Unnecessary space(s)")
        }
    }

    @Test
    fun `should succeed check on clean sources`() {

        projectRoot.withCleanSources()

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    fun `should generate code style files in project`() {
        projectRoot.withCleanSources()
        val ideaRootDir = projectRoot.resolve(".idea").apply { mkdir() }

        build(APPLY_TO_IDEA_TASK_NAME).apply {
            assertThat(task(":$APPLY_TO_IDEA_TASK_NAME")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(ideaRootDir.listFiles()?.isNullOrEmpty()).isFalse()
        }
    }

    @Test
    fun `should generate code style file globally`() {
        val ideaRootDir = projectRoot.resolve(".idea").apply { mkdir() }

        build(APPLY_TO_IDEA_GLOBALLY_TASK_NAME).apply {
            assertThat(task(":$APPLY_TO_IDEA_GLOBALLY_TASK_NAME")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(ideaRootDir.listFiles()?.isNullOrEmpty()).isFalse()
        }
    }

    @Test
    fun `should show only plugin meta tasks in task output`() {
        projectRoot.withCleanSources()

        build("tasks").apply {
            val ktlintTasks = output
                .lineSequence()
                .filter { it.startsWith("ktlint", ignoreCase = true) }
                .toList()

            assertThat(ktlintTasks).hasSize(4)
            assertThat(ktlintTasks).anyMatch { it.startsWith(CHECK_PARENT_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(FORMAT_PARENT_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(APPLY_TO_IDEA_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(APPLY_TO_IDEA_GLOBALLY_TASK_NAME) }
        }
    }

    @Test
    fun `should show all ktlint tasks in task output`() {
        build("tasks", "--all").apply {
            val ktlintTasks = output
                .lineSequence()
                .filter { it.startsWith("ktlint", ignoreCase = true) }
                .toList()

            // Plus for main and test sources format and check tasks
            // Plus two kotlin script tasks
            assertThat(ktlintTasks).hasSize(10)
            assertThat(ktlintTasks).anyMatch { it.startsWith(CHECK_PARENT_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(FORMAT_PARENT_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(APPLY_TO_IDEA_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(APPLY_TO_IDEA_GLOBALLY_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(kotlinScriptCheckTaskName) }
            assertThat(ktlintTasks).anyMatch {
                it.startsWith(
                    GenerateReportsTask.generateNameForKotlinScripts(GenerateReportsTask.LintType.FORMAT)
                )
            }
        }
    }

    @Test
    fun `Should ignore excluded sources`() {
        projectRoot.withCleanSources()
        projectRoot.withFailingSources()

        projectRoot.buildFile().appendText(
            """

            ktlint.filter { exclude("**/fail-source.kt") }
            """.trimIndent()
        )

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    fun `Should fail on additional source set directories files style violation`() {
        projectRoot.withCleanSources()
        val alternativeDirectory = "src/main/shared"
        projectRoot.withAlternativeFailingSources(alternativeDirectory)

        projectRoot.buildFile().appendText(
            """

            sourceSets {
                findByName("main")?.java?.srcDirs(project.file("$alternativeDirectory"))
            }
            """.trimIndent()
        )

        buildAndFail(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
        }
    }

    @Test
    fun `Should always format again restored to pre-format state sources`() {
        projectRoot.withFailingSources()
        val formatTaskName = KtLintFormatTask.buildTaskNameForSourceSet("main")
        build(FORMAT_PARENT_TASK_NAME).apply {
            assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        projectRoot.restoreFailingSources()

        build(FORMAT_PARENT_TASK_NAME).apply {
            assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
        assertThat(projectRoot.resolve(FAIL_SOURCE_FILE)).exists()
    }

    @Test
    fun `Format task should be up-to-date on 3rd run`() {
        projectRoot.withFailingSources()
        val formatTaskName = KtLintFormatTask.buildTaskNameForSourceSet("main")

        build(FORMAT_PARENT_TASK_NAME).apply {
            assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
        build(FORMAT_PARENT_TASK_NAME).apply {
            assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
        build(FORMAT_PARENT_TASK_NAME).apply {
            assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }
    }

    @Test
    fun `Should apply ktLint version from extension`() {
        projectRoot.withCleanSources()

        projectRoot.buildFile().appendText(
            """

            ktlint.version = "0.35.0"
            """.trimIndent()
        )

        build(":dependencies").apply {
            assertThat(output).contains(
                "$KTLINT_CONFIGURATION_NAME - $KTLINT_CONFIGURATION_DESCRIPTION${System.lineSeparator()}" +
                    "\\--- com.pinterest:ktlint:0.35.0${System.lineSeparator()}"
            )
        }
    }

    @Test
    internal fun `Should check kotlin script file in project folder`() {
        projectRoot.withCleanSources()
        projectRoot.withCleanKotlinScript()

        build(kotlinScriptCheckTaskName).apply {
            assertThat(task(":$kotlinScriptCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Should fail check of kotlin script file in project folder`() {
        projectRoot.withCleanSources()
        projectRoot.withFailingKotlinScript()

        buildAndFail(kotlinScriptCheckTaskName).apply {
            assertThat(task(":$kotlinScriptCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
        }
    }

    @Test
    internal fun `Should not check kotlin script file in child project folder`() {
        projectRoot.withCleanSources()
        val additionalFolder = projectRoot.resolve("scripts/")
        additionalFolder.withFailingKotlinScript()

        build(kotlinScriptCheckTaskName).apply {
            assertThat(task(":$kotlinScriptCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SKIPPED)
        }
    }

    @Test
    internal fun `Should check kts file in configured child project folder`() {
        projectRoot.withCleanSources()
        val additionalFolder = projectRoot.resolve("scripts/")
        additionalFolder.withCleanKotlinScript()
        projectRoot.buildFile().appendText(
            """

            ktlint.kotlinScriptAdditionalPaths { include fileTree("scripts/") }
            """.trimIndent()
        )

        build(kotlinScriptCheckTaskName).apply {
            assertThat(task(":$kotlinScriptCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Should apply internal git filter to check task`() {
        projectRoot.withCleanSources()
        projectRoot.withFailingSources()

        build(
            ":$CHECK_PARENT_TASK_NAME",
            "-P$FILTER_INCLUDE_PROPERTY_NAME=src/main/kotlin/clean-source.kt"
        ).run {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Should internal git filter work with Windows`() {
        projectRoot.withCleanSources()
        projectRoot.withFailingSources()

        build(
            ":$CHECK_PARENT_TASK_NAME",
            "-P$FILTER_INCLUDE_PROPERTY_NAME=src\\main\\kotlin\\clean-source.kt"
        ).run {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Git filter should respect already applied filters`() {
        projectRoot.withFailingSources()
        projectRoot.buildFile().appendText(
            """

            ktlint.filter { exclude("**/fail-source.kt") }
            """.trimIndent()
        )

        build(
            ":$CHECK_PARENT_TASK_NAME",
            "-P$FILTER_INCLUDE_PROPERTY_NAME=src/main/kotlin/fail-source.kt"
        ).run {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SKIPPED)
        }
    }

    @Test
    internal fun `Git filter should ignore task if no files related to it`() {
        projectRoot.withCleanSources()

        build(
            ":$CHECK_PARENT_TASK_NAME",
            "-P$FILTER_INCLUDE_PROPERTY_NAME=src/main/kotlin/failing-sources.kt"
        ).run {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SKIPPED)
        }
    }

    @Test
    internal fun `Should enable experimental indentation rule`() {
        projectRoot.createSourceFile(
            "src/main/kotlin/C.kt",
            """
                class C {

                    private val Any.className
                        get() = this.javaClass.name
                            .fn()

                    private fun String.escape() =
                        this.fn()
                }
            """.trimIndent()
        )
        projectRoot.buildFile().appendText(
            """

            ktlint.enableExperimentalRules = true
            ktlint.version = "0.34.0"
            """.trimIndent()
        )

        buildAndFail(":$CHECK_PARENT_TASK_NAME", "-s").apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
        }
    }

    @Test
    internal fun `Lint check should run incrementally`() {
        val initialSourceFile = "src/main/kotlin/initial.kt"
        projectRoot.createSourceFile(
            initialSourceFile,
            """
            val foo = "bar"
            
            """.trimIndent()
        )

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        val additionalSourceFile = "src/main/kotlin/another-file.kt"
        projectRoot.createSourceFile(
            additionalSourceFile,
            """
            val bar = "foo"
            
            """.trimIndent()
        )

        build(CHECK_PARENT_TASK_NAME, "--info").apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains("Executing incrementally")
        }
    }

    @Test
    internal fun `Should check files which path contains whitespace`() {
        projectRoot.createSourceFile(
            "src/main/kotlin/some path with whitespace/some file.kt",
            """
            class Test
            """.trimIndent()
        )

        buildAndFail(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            // TODO: see https://github.com/pinterest/ktlint/issues/997 why check was changed
            // should be reverted back once next ktlint release is out
            assertThat(output).contains(
                "File must end with a newline (\\n)"
            )
        }
    }

    @Test
    internal fun `Should do nothing when there are no eligible incremental updates`() {
        val passingContents =
            """
            val foo = "bar"

            """.trimIndent()

        val failingContents =
            """
            val foo="bar"

            """.trimIndent()

        val initialSourceFile = "src/main/kotlin/initial.kt"
        projectRoot.createSourceFile(initialSourceFile, passingContents)

        val additionalSourceFile = "src/main/kotlin/another-file.kt"
        projectRoot.createSourceFile(additionalSourceFile, passingContents)

        val testSourceFile = "src/test/kotlin/another-file.kt"
        projectRoot.createSourceFile(testSourceFile, failingContents)

        build(mainSourceSetCheckTaskName).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        // Removing a source file will cause the task to run, but the only incremental change will
        // be REMOVED, which does need to call ktlint
        projectRoot.removeSourceFile(initialSourceFile)
        build(mainSourceSetCheckTaskName).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }
    }

    @Test
    internal fun `Should not leak KtLint into buildscript classpath`() {
        projectRoot.withCleanSources()

        build("buildEnvironment").apply {
            assertThat(output).doesNotContain("com.pinterest.ktlint")
        }
    }

    @Test
    internal fun `Should print pathes to generated reports on code style violations`() {
        projectRoot.withFailingSources()

        buildAndFail(CHECK_PARENT_TASK_NAME).apply {
            val s = File.separator
            assertThat(output).contains(
                "build${s}reports${s}ktlint${s}ktlintMainSourceSetCheck${s}ktlintMainSourceSetCheck.txt"
            )
        }
    }

    @Test
    internal fun `Should force dependencies versions from KtLint configuration for ruleset configuration`() {
        projectRoot.withCleanSources()

        projectRoot.buildFile().appendText(
            """

            dependencies {
                $KTLINT_RULESET_CONFIGURATION_NAME "com.pinterest.ktlint:ktlint-core:0.34.2"
            }
            """.trimIndent()
        )

        build(":dependencies", "--configuration", KTLINT_RULESET_CONFIGURATION_NAME).apply {
            assertThat(output).contains("com.pinterest.ktlint:ktlint-core:0.34.2 -> 0.41.0")
        }
    }

    @Test
    internal fun `Should force dependencies versions from KtLint configuration for reporters configuration`() {
        projectRoot.withCleanSources()

        projectRoot.buildFile().appendText(
            """

            dependencies {
                $KTLINT_REPORTER_CONFIGURATION_NAME "com.pinterest.ktlint:ktlint-core:0.34.2"
            }
            """.trimIndent()
        )

        build(":dependencies", "--configuration", KTLINT_REPORTER_CONFIGURATION_NAME).apply {
            assertThat(output).contains("com.pinterest.ktlint:ktlint-core:0.34.2 -> 0.41.0")
        }
    }
}
