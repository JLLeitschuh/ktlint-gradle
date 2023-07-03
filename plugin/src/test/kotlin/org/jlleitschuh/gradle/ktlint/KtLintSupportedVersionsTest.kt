package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.testdsl.GradleArgumentsProvider
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.TestProject
import org.jlleitschuh.gradle.ktlint.testdsl.TestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
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
                ktlint.additionalEditorconfigFile = project.file("${TestProject.ADDITIONAL_EDITOR_CONFIG}/.editorconfig")
                """.trimIndent()
            )
            withAdditionalEditorConfig()
            withFailingSources()
            if (SemVer.parse(ktLintVersion) >= SemVer(0, 47)) {
                buildAndFail(CHECK_PARENT_TASK_NAME) {
                    assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
                    assertThat(output.contains("additionalEditorconfigFile no longer supported in ktlint 0.47+"))
                }
            } else {
                build(CHECK_PARENT_TASK_NAME) {
                    assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
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
            "0.34.0",
            "0.34.2",
            "0.35.0",
            "0.36.0",
            "0.37.1",
            "0.37.2",
            "0.38.0",
            "0.38.1",
            "0.39.0",
            "0.40.0",
            "0.41.0",
            "0.42.0",
            "0.42.1",
            // "0.43.0" does not work on JDK1.8
            // "0.43.1" asked not to use it
            "0.43.2",
            "0.44.0",
            "0.45.2",
            "0.46.1",
            "0.47.1",
            "0.48.0",
            "0.48.1",
            "0.48.2",
            // "0.49.0" did not expose needed baseline classes
            "0.49.1",
            "0.50.0"
        ).also {
            // "0.37.0" is failing on Windows machines that is fixed in the next version
            if (!OS.WINDOWS.isCurrentOs) it.add("0.37.0")
        }

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
