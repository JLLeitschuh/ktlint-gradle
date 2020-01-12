package org.jlleitschuh.gradle.ktlint

import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    @BeforeEach
    internal fun setUp() {
        projectRoot.defaultProjectSetup()
    }

    @Test
    fun `Check task should be up_to_date if editorconfig content not changed`() {
        projectRoot.withCleanSources()
        projectRoot.createEditorconfigFile()

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }
    }

    @Test
    internal fun `Check task should be up_to_date if additional editorconfig content not changed`() {
        val additionalConfigPath = temporaryFolder.resolve("some/additional/folder/").toString()
        projectRoot.withCleanSources()
        projectRoot.createEditorconfigFile(filePath = additionalConfigPath)

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }
    }

    @Test
    fun `Check task should rerun if editorconfig content changed`() {
        projectRoot.withCleanSources()
        projectRoot.createEditorconfigFile()

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        projectRoot.modifyEditorconfigFile(100)
        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Check task should rerun if additional editorconfig content changed`() {
        val additionalConfigPath = temporaryFolder.resolve("some/additional/folder").toString()
        projectRoot.withCleanSources()
        projectRoot.createEditorconfigFile(filePath = additionalConfigPath)

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        projectRoot.modifyEditorconfigFile(
            maxLineLength = 100,
            filePath = additionalConfigPath
        )
        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Check task should rerun if root editorconfig content changed`() {
        val projectWithModulesLocation = temporaryFolder.resolve("modularized")
        projectWithModulesLocation.mkdirs()
        val moduleLocation = projectWithModulesLocation.resolve("test/module1")
        moduleLocation.mkdirs()

        projectWithModulesLocation.settingsFile().writeText("""
            include ":test:module1"
        """.trimIndent())
        projectWithModulesLocation.buildFile().writeText("""
            ${pluginsBlockWithMainPluginAndKotlinJvm()}

            repositories {
                gradlePluginPortal()
            }

            allprojects {
                repositories {
                    jcenter()
                }
            }
        """.trimIndent())
        projectWithModulesLocation.createEditorconfigFile()
        moduleLocation.buildFile().writeText("""
            apply plugin: "kotlin"
            apply plugin: "org.jlleitschuh.gradle.ktlint"
        """.trimIndent())
        moduleLocation.withCleanSources()

        val gradleRunner = GradleRunner.create()
            .withProjectDir(projectWithModulesLocation)
            .withPluginClasspath()

        gradleRunner
            .withArguments(":test:module1:ktlintCheck")
            .forwardOutput()
            .build().apply {
                assertThat(task(":test:module1:ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

        projectWithModulesLocation.modifyEditorconfigFile(100)

        gradleRunner
            .withArguments(":test:module1:ktlintCheck")
            .forwardOutput()
            .build().apply {
                assertThat(task(":test:module1:ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
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
