package org.jlleitschuh.gradle.ktlint

import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FutureGradleConfigurationCacheTest : ConfigurationCacheTest() {
    override fun gradleRunnerFor(
        vararg arguments: String,
        projectRoot: File
    ): GradleRunner =
        super.gradleRunnerFor(*arguments, projectRoot = projectRoot)
            .withGradleVersion("6.6-milestone-3")
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
    internal fun setUp() {
        projectRoot.defaultProjectSetup(kotlinVersion = "1.4-M3")
    }

    @Test
    internal fun `Should support configuration cache without errors when checking`() {
        projectRoot.createSourceFile(
            "src/main/kotlin/clean-source.kt",
            """
            val foo = "bar"
            
            """.trimIndent()
        )
        build(configurationCacheFlag, configurationCacheWarnFlag, maxProblemsFlag, ":ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
        build(configurationCacheFlag, configurationCacheWarnFlag, maxProblemsFlag, ":ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            assertThat(output).contains("Reusing configuration cache.")
        }
    }
    @Test
    internal fun `Should support configuration cache without errors when reformating`() {
        projectRoot.createSourceFile(
            "src/main/kotlin/clean-source.kt",
            """
            val foo = "bar"
            """.trimIndent()
        )
        build(configurationCacheFlag, configurationCacheWarnFlag, maxProblemsFlag, ":ktlintFormat").apply {
            assertThat(task(":ktlintMainSourceSetFormat")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
        build(configurationCacheFlag, configurationCacheWarnFlag, maxProblemsFlag, ":ktlintFormat").apply {
            assertThat(task(":ktlintMainSourceSetFormat")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(output).contains("Reusing configuration cache.")
        }
    }
}
