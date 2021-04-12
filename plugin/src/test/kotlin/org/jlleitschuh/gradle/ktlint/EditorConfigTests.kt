package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jlleitschuh.gradle.ktlint.KtlintBasePlugin.Companion.LOWEST_SUPPORTED_GRADLE_VERSION
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Runs [EditorConfigTests] with the current version of Gradle.
 */
class GradleCurrentEditorConfigTest : EditorConfigTests()

/**
 * Runs [EditorConfigTests] with lowest supported Gradle version.
 */
@Suppress("ClassName")
class GradleLowestSupportedEditorConfigTest : EditorConfigTests() {

    override fun gradleRunnerFor(
        vararg arguments: String,
        projectRoot: File
    ): GradleRunner =
        super.gradleRunnerFor(*arguments, projectRoot = projectRoot)
            .withGradleVersion(LOWEST_SUPPORTED_GRADLE_VERSION)
}

/**
 * Contains all tests related to `.editorconfig` files support.
 */
abstract class EditorConfigTests : AbstractPluginTest() {
    private val lintTaskName = KtLintCheckTask.buildTaskNameForSourceSet("main")

    @BeforeEach
    internal fun setUp() {
        projectRoot.defaultProjectSetup()
    }

    @Test
    fun `Check task should be up_to_date if editorconfig content not changed`() {
        projectRoot.withCleanSources()
        projectRoot.createEditorconfigFile()

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }
    }

    @Test
    internal fun `Check task should be up_to_date if additional editorconfig content not changed`() {
        val additionalConfigPath = temporaryFolder.resolve("some/additional/folder/").toString()
        projectRoot.withCleanSources()
        projectRoot.createEditorconfigFile(filePath = additionalConfigPath)

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }
    }

    @Test
    fun `Check task should rerun if editorconfig content changed`() {
        projectRoot.withCleanSources()
        projectRoot.createEditorconfigFile()

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        projectRoot.modifyEditorconfigFile(100)
        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Check task should rerun if additional editorconfig content changed`() {
        val additionalConfigPath = temporaryFolder.resolve("some/additional/folder").toString()
        projectRoot.withCleanSources()
        projectRoot.createEditorconfigFile(filePath = additionalConfigPath)

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        projectRoot.modifyEditorconfigFile(
            maxLineLength = 100,
            filePath = additionalConfigPath
        )
        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Check task should rerun if root editorconfig content changed`() {
        val projectWithModulesLocation = temporaryFolder.resolve("modularized")
        projectWithModulesLocation.mkdirs()
        val moduleLocation = projectWithModulesLocation.resolve("test/module1")
        moduleLocation.mkdirs()

        projectWithModulesLocation.settingsFile().writeText(
            """
            include ":test:module1"
            """.trimIndent()
        )
        projectWithModulesLocation.buildFile().writeText(
            """
            ${pluginsBlockWithMainPluginAndKotlinJvm()}

            repositories {
                gradlePluginPortal()
            }

            allprojects {
                repositories {
                    mavenCentral()
                }
            }
            """.trimIndent()
        )
        projectWithModulesLocation.createEditorconfigFile()
        moduleLocation.buildFile().writeText(
            """
            apply plugin: "kotlin"
            apply plugin: "org.jlleitschuh.gradle.ktlint"
            """.trimIndent()
        )
        moduleLocation.withCleanSources()

        val gradleRunner = GradleRunner.create()
            .withProjectDir(projectWithModulesLocation)
            .withPluginClasspath()

        gradleRunner
            .withArguments(":test:module1:$CHECK_PARENT_TASK_NAME")
            .forwardOutput()
            .build().apply {
                assertThat(task(":test:module1:$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

        projectWithModulesLocation.modifyEditorconfigFile(100)

        gradleRunner
            .withArguments(":test:module1:$CHECK_PARENT_TASK_NAME")
            .forwardOutput()
            .build().apply {
                assertThat(task(":test:module1:$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
    }

    private fun File.createEditorconfigFile(
        maxLineLength: Int = 120,
        filePath: String = ""
    ) = createSourceFile(
        "$filePath.editorconfig",
        """
            [*.{kt,kts}]
            max_line_length=$maxLineLength
        """.trimIndent()
    )

    private fun File.modifyEditorconfigFile(
        maxLineLength: Int,
        filePath: String = ""
    ) {
        val editorconfigFile = resolve("$filePath.editorconfig")
        if (editorconfigFile.exists()) {
            editorconfigFile.delete()
        }
        createEditorconfigFile(maxLineLength)
    }
}
