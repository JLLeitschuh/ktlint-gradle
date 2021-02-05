package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import java.io.File

class UnsupportedGradleTest : AbstractPluginTest() {
    override fun gradleRunnerFor(
        vararg arguments: String,
        projectRoot: File
    ): GradleRunner =
        super.gradleRunnerFor(*arguments, projectRoot = projectRoot)
            .withGradleVersion("5.6.4")

    @Test
    internal fun `Should raise exception on applying plugin`() {
        projectRoot.defaultProjectSetup()

        buildAndFail(CHECK_PARENT_TASK_NAME).run {
            assertThat(output).contains(
                "Current version of plugin supports minimal Gradle version: " +
                    KtlintBasePlugin.LOWEST_SUPPORTED_GRADLE_VERSION
            )
        }
    }
}
