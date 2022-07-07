package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.testdsl.GradleArgumentsProvider
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.TestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream
import kotlin.streams.asStream

@GradleTestVersions
class KtLintSupportedVersionsTest : AbstractPluginTest() {

    @DisplayName("Should lint correct sources without errors")
    @ParameterizedTest(name = "{0} with KtLint {1}: {displayName}")
    @ArgumentsSource(SupportedKtlintVersionsProvider::class)
    internal fun `Should lint correct sources without errors`(
        gradleVersion: GradleVersion,
        ktLintVersion: String
    ) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.version = "$ktLintVersion"
                """.trimIndent()
            )

            withCleanSources()

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    @DisplayName("Lint should fail on sources with style violations")
    @ParameterizedTest(name = "{0} with KtLint {1}: {displayName}")
    @ArgumentsSource(SupportedKtlintVersionsProvider::class)
    internal fun `Lint should fail on sources with style violations`(
        gradleVersion: GradleVersion,
        ktLintVersion: String
    ) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.version = "$ktLintVersion"

                """.trimIndent()
            )

            withFailingSources()

            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            }
        }
    }

    @DisplayName("Format should successfully finish on sources with style violations")
    @ParameterizedTest(name = "{0} with KtLint {1}: {displayName}")
    @ArgumentsSource(SupportedKtlintVersionsProvider::class)
    internal fun `Format should successfully finish on sources with style violations`(
        gradleVersion: GradleVersion,
        ktLintVersion: String
    ) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
                """
                ktlint.version = "$ktLintVersion"

                """.trimIndent()
            )

            withFailingSources()

            build(FORMAT_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetFormatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    class SupportedKtlintVersionsProvider : GradleArgumentsProvider() {
        private val supportedKtlintVersions = mutableListOf(
            "0.46.1"
        )

        override fun provideArguments(
            context: ExtensionContext
        ): Stream<out Arguments> {
            // Using multiple Gradle versions produces OOM exceptions on Windows OS
            return sequenceOf(GradleVersion.version(TestVersions.maxSupportedGradleVersion))
                .flatMap { gradleVersion ->
                    supportedKtlintVersions.map { gradleVersion to it }.asSequence()
                }
                .map {
                    Arguments.of(it.first, it.second)
                }
                .asStream()
        }
    }
}
