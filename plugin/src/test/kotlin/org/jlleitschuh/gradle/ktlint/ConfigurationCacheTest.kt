package org.jlleitschuh.gradle.ktlint

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class FutureGradleConfigurationCacheTest: ConfigurationCacheTest() {
    override fun gradleRunnerFor(
        vararg arguments: String,
        projectRoot: File
    ): GradleRunner =
        super.gradleRunnerFor(*arguments, projectRoot = projectRoot)
            .withGradleVersion("6.6-milestone-3")
}

abstract class ConfigurationCacheTest: AbstractPluginTest() {
    @BeforeEach
    internal fun setUp() {
        projectRoot.defaultProjectSetup(kotlinVersion = "1.4-M3")
    }

    @Test
    internal fun `Should support configuration cache without errors`() {
        projectRoot.createSourceFile(
            "src/main/kotlin/clean-source.kt",
            """
            val foo = "bar"
            """.trimIndent()
        )

        build("--configuration-cache", ":ktlintCheck").apply {

        }
    }
}
