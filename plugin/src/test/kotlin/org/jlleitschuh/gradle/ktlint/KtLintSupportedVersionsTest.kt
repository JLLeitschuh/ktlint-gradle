package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
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

    @DisplayName("Lint should use editorconfig override (standard rule)")
    @ParameterizedTest(name = "{0} with KtLint {1}: {displayName}")
    @ArgumentsSource(SupportedKtlintVersionsProvider::class)
    internal fun `Lint should use editorconfig override standard rule`(
        gradleVersion: GradleVersion,
        ktLintVersion: String
    ) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
                """
                ktlint.version = "$ktLintVersion"
                ktlint.additionalEditorconfig = [
                            "ktlint_standard_no-multi-spaces": "disabled"
                ]
                """.trimIndent()
            )
            withFailingSources()
            if (SemVer.parse(ktLintVersion) < SemVer(0, 49, 0)) {
                buildAndFail(CHECK_PARENT_TASK_NAME) {
                    assertThat(task(":$mainSourceSetCheckTaskName")?.outcome)
                        .`as`("additionalEditorconfig not supported until ktlint 0.49")
                        .isEqualTo(TaskOutcome.FAILED)
                    assertThat(output).contains("additionalEditorconfig not supported until ktlint 0.49")
                }
            } else if (SemVer.parse(ktLintVersion) < SemVer(1, 0)) {
                buildAndFail(CHECK_PARENT_TASK_NAME) {
                    assertThat(task(":runKtlintCheckOverMainSourceSet")?.outcome)
                        .`as`("standard rules not supported by additionalEditorconfig until 1.0")
                        .isEqualTo(TaskOutcome.FAILED)
                    assertThat(output)
                        .contains("Property with name 'ktlint_standard_no-multi-spaces' is not found")
                }
            } else {
                build(CHECK_PARENT_TASK_NAME) {
                    assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                }
            }
        }
    }

    @DisplayName("Lint should use editorconfig override")
    @ParameterizedTest(name = "{0} with KtLint {1}: {displayName}")
    @ArgumentsSource(SupportedKtlintVersionsProvider::class)
    internal fun `Lint should use editorconfig override`(
        gradleVersion: GradleVersion,
        ktLintVersion: String
    ) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.version = "$ktLintVersion"
                ktlint.additionalEditorconfig = [
                            "max_line_length": "20"
                ]
                """.trimIndent()
            )
            withFailingMaxLineSources()
            if (SemVer.parse(ktLintVersion) < SemVer(0, 49)) {
                build(CHECK_PARENT_TASK_NAME) {
                    assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                    assertThat(output).contains("additionalEditorconfig not supported until ktlint 0.49")
                }
            } else {
                buildAndFail(CHECK_PARENT_TASK_NAME) {
                    assertThat(task(":$mainSourceSetCheckTaskName")?.outcome)
                        .`as`("additionalEditorconfig takes effect")
                        .isEqualTo(TaskOutcome.FAILED)
                    assertThat(output).doesNotContain("additionalEditorconfig not supported until ktlint 0.49")
                    assertThat(output).contains("Exceeded max line length (20) (cannot be auto-corrected)")
                }
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
            "0.47.1",
            "0.48.2",
            // "0.49.0" did not expose needed baseline classes
            "0.49.1",
            "0.50.0",
            "1.0.1",
            "1.1.1",
            "1.2.1"
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
