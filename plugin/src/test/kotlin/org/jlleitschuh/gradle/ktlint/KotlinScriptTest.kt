package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jlleitschuh.gradle.ktlint.KtlintBasePlugin.Companion.LOWEST_SUPPORTED_GRADLE_VERSION
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Runs the tests with the current version of Gradle.
 */
class GradleCurrentKotlinScriptTest : BaseKotlinScriptTest()

@Suppress("ClassName")
class GradleLowestSupportedKotlinScriptTest : BaseKotlinScriptTest() {

    override fun gradleRunnerFor(
        vararg arguments: String,
        projectRoot: File
    ): GradleRunner =
        super.gradleRunnerFor(*arguments, projectRoot = projectRoot)
            .withGradleVersion(LOWEST_SUPPORTED_GRADLE_VERSION)
}

abstract class BaseKotlinScriptTest : AbstractPluginTest() {
    @BeforeEach
    internal fun setUp() {
        projectRoot.defaultProjectSetupKS()
    }

    @Test
    internal fun `Should not lint kotlin script files from subprojects`() {
        val subprojectRoot = projectRoot.resolve("subProject1").also { it.mkdirs() }
        subprojectRoot.withCleanSources()
        subprojectRoot.defaultProjectSetupKS()

        projectRoot.settingsFile().appendText("include(\":subProject1\")")
        projectRoot.withCleanSources()
        projectRoot.buildFileKS().appendText(
            """
            
            ktlint.debug.set(true)
            
            """.trimIndent()
        )

        build(":$kotlinScriptCheckTaskName").apply {
            assertThat(task(":$kotlinScriptCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).doesNotContain(subprojectRoot.buildFileKS().absolutePath)
        }
    }
}
