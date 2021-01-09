package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class KtLintSupportedVersionsTest : AbstractPluginTest() {

    @BeforeEach
    internal fun setUp() {
        projectRoot.defaultProjectSetup()
    }

    @ParameterizedTest
    @MethodSource("provideSupportedKtLintVersions")
    internal fun `Should lint correct sources without errors`(
        ktLintVersion: String
    ) {
        projectRoot.buildFile().appendText(
            """

            ktlint.version = "$ktLintVersion"
            """.trimIndent()
        )

        projectRoot.withCleanSources()

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @ParameterizedTest
    @MethodSource("provideSupportedKtLintVersions")
    internal fun `Lint should fail on sources with style violations`(
        ktLintVersion: String
    ) {
        projectRoot.buildFile().appendText(
            """

            ktlint.version = "$ktLintVersion"
            """.trimIndent()
        )

        projectRoot.withFailingSources()

        buildAndFail(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
        }
    }

    @ParameterizedTest
    @MethodSource("provideSupportedKtLintVersions")
    internal fun `Format should successfully finish on sources with style violations`(
        ktLintVersion: String
    ) {
        projectRoot.buildFile().appendText(
            """

            ktlint.version = "$ktLintVersion"
            """.trimIndent()
        )

        projectRoot.withFailingSources()

        build(FORMAT_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetFormatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun provideSupportedKtLintVersions(): Stream<Arguments> {
            val versions = mutableListOf(
                Arguments.of("0.34.0"),
                Arguments.of("0.34.2"),
                Arguments.of("0.35.0"),
                Arguments.of("0.36.0"),
                Arguments.of("0.37.1"),
                Arguments.of("0.37.2"),
                // "0.38.0" has been compiled with Kotlin apiLevel 1.4 and not supported by Gradle plugins
                Arguments.of("0.38.1"),
                Arguments.of("0.39.0"),
                Arguments.of("0.40.0")
            )

            // "0.37.0" is failing on Windows machines that is fixed in the next version
            if (!OS.WINDOWS.isCurrentOs) versions.add(Arguments.of("0.37.0"))

            return Stream.of(*versions.toTypedArray())
        }
    }
}
