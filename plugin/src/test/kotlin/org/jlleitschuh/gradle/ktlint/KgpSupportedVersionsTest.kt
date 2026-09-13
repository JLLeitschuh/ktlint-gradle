package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.TestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.jlleitschuh.gradle.ktlint.testdsl.projectSetup
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class KgpSupportedVersionsTest : AbstractPluginTest() {

    @ParameterizedTest
    @ValueSource(strings = [TestVersions.minSupportedKotlinPluginVersion, TestVersions.maxSupportedKotlinPluginVersion])
    internal fun `Should lint correct sources without errors`(kgpVersion: String) {
        project(
            gradleVersion = GradleVersion.version(TestVersions.maxSupportedGradleVersion),
            projectSetup = projectSetup("jvm", kgpVersion)
        ) {
            withCleanSources()
            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [TestVersions.minSupportedKotlinPluginVersion, TestVersions.maxSupportedKotlinPluginVersion])
    internal fun `Lint should fail on sources with style violations`(kgpVersion: String) {
        project(
            gradleVersion = GradleVersion.version(TestVersions.maxSupportedGradleVersion),
            projectSetup = projectSetup("jvm", kgpVersion)
        ) {
            withFailingSources()
            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [TestVersions.minSupportedKotlinPluginVersion, TestVersions.maxSupportedKotlinPluginVersion])
    internal fun `Format should successfully finish on sources with style violations`(kgpVersion: String) {
        project(
            gradleVersion = GradleVersion.version(TestVersions.maxSupportedGradleVersion),
            projectSetup = projectSetup("jvm", kgpVersion)
        ) {
            withFailingSources()
            build(FORMAT_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetFormatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }
}
