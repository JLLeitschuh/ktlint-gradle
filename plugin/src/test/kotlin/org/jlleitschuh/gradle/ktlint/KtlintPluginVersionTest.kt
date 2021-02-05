package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.jlleitschuh.gradle.ktlint.tasks.LoadReportersTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class KtlintPluginVersionTest : AbstractPluginTest() {

    private fun File.buildScriptUsingKtlintVersion(version: String) {
        buildFile().writeText(
            """
                ${pluginsBlockWithMainPluginAndKotlinJvm()}

                repositories {
                    gradlePluginPortal()
                }

                buildDir = file("directory with spaces")

                ktlint {
                    version = "$version"
                }
            """.trimIndent()
        )
    }

    @BeforeEach
    fun setup() {
        projectRoot.withCleanSources()
    }

    @Test
    fun `with ktLint version equal to 0_34`() {
        projectRoot.buildScriptUsingKtlintVersion("0.34.0")
        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    fun `with ktLint version less than 0_34`() {
        projectRoot.buildScriptUsingKtlintVersion("0.33.0")
        buildAndFail(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":${LoadReportersTask.TASK_NAME}")?.outcome).isEqualTo(TaskOutcome.FAILED)
        }
    }
}
