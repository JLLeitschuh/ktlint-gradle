package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class GradleCurrentConfigurationTest : ConfigurationCacheTest() {
    override fun gradleRunnerFor(vararg arguments: String, projectRoot: File): GradleRunner {
        return super.gradleRunnerFor(*arguments, projectRoot = projectRoot)
            .withGradleVersion("6.8-rc-5")
    }
}

abstract class ConfigurationCacheTest : AbstractPluginTest() {
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

    @BeforeEach
    fun setupBuild() {
        projectRoot.defaultProjectSetup()
    }

    @Test
    internal fun `Should support configuration cache without errors when checking`() {
        projectRoot.createSourceFile(
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
        ).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        build(
            configurationCacheFlag,
            configurationCacheWarnFlag,
            maxProblemsFlag,
            CHECK_PARENT_TASK_NAME
        ).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            assertThat(output).contains("Reusing configuration cache.")
        }
    }
    @Test
    internal fun `Should support configuration cache without errors when reformatting`() {
        projectRoot.createSourceFile(
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
        ).apply {
            assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(task(":$mainSourceSetFormatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        build(
            configurationCacheFlag,
            configurationCacheWarnFlag,
            maxProblemsFlag,
            FORMAT_PARENT_TASK_NAME
        ).apply {
            assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(task(":$mainSourceSetFormatTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            assertThat(output).contains("Reusing configuration cache.")
        }
    }
}
