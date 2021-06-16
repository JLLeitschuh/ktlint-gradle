package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File

@Disabled(
    "Kotlin Gradle plugin version is leaking into test project via 'GradleRunner.withPluginClasspath()'" +
        " not allowing to set lower version."
)
class UnsupportedGradleTest : AbstractPluginTest() {
    override fun gradleRunnerFor(
        vararg arguments: String,
        projectRoot: File
    ): GradleRunner =
        super.gradleRunnerFor(*arguments, projectRoot = projectRoot)
            .withGradleVersion("5.6.4")

    @Test
    @DisabledOnOs(OS.WINDOWS)
    internal fun `Should raise exception on applying plugin`() {
        projectRoot.defaultProjectSetup(kotlinVersion = "1.4.32")

        buildAndFail(CHECK_PARENT_TASK_NAME).run {
            assertThat(output).contains(
                "Current version of plugin supports minimal Gradle version: " +
                    KtlintBasePlugin.LOWEST_SUPPORTED_GRADLE_VERSION
            )
        }
    }
}
