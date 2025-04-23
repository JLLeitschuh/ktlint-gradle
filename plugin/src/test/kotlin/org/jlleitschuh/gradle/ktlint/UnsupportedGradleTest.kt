package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.getMajorJavaVersion
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.jlleitschuh.gradle.ktlint.testdsl.projectSetup
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS

class UnsupportedGradleTest : AbstractPluginTest() {
    @DisplayName("Should raise exception on applying plugin for build using unsupported Gradle version.")
    @Test
    @DisabledOnOs(OS.WINDOWS)
    internal fun errorOnOldGradleVersion() {
        /**
         * This test ensures the proper error message is printed when an unsupported version of gradle is used.
         * However, our minimum version of gradle is still 7.x, which will not run at all on Java 21.
         * Gradle 8.5 is needed for Java 21.
         * So if java 21 is currently being used, skip this test
         */
        Assumptions.assumeFalse(getMajorJavaVersion() >= 21)

        project(
            gradleVersion = GradleVersion.version("7.4.1"),
            projectSetup = projectSetup("jvm", "1.9.22")
        ) {
            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(output).contains(
                    "Current version of plugin supports minimal Gradle version: " +
                        KtlintBasePlugin.LOWEST_SUPPORTED_GRADLE_VERSION
                )
            }
        }
    }
}
