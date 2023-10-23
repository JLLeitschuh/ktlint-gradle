package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

class UnsupportedGradleTest : AbstractPluginTest() {
    @DisplayName("Should raise exception on applying plugin for build using unsupported Gradle version.")
    @Test
    @DisabledOnOs(OS.WINDOWS)
    internal fun errorOnOldGradleVersion() {
        project(GradleVersion.version("6.9.2")) {
            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(output).contains(
                    "Current version of plugin supports minimal Gradle version: " +
                        KtlintBasePlugin.LOWEST_SUPPORTED_GRADLE_VERSION
                )
            }
        }
    }
}
