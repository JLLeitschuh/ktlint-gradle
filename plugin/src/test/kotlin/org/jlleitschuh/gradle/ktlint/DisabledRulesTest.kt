package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

@GradleTestVersions
class DisabledRulesTest : AbstractPluginTest() {

    @DisplayName("Should lint without errors when 'final-newline' rule is disabled")
    @ParameterizedTest(name = "{0} with KtLint {1}: {displayName}")
    @ArgumentsSource(KtLintSupportedVersionsTest.SupportedKtlintVersionsProvider::class)
    fun lintDisabledRuleFinalNewline(gradleVersion: GradleVersion, ktLintVersion: String) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
                """
                ktlint {
                    version = "$ktLintVersion"
                    disabledRules = ["final-newline"]
                }
                """.trimIndent()
            )

            createSourceFile(
                "src/main/kotlin/CleanSource.kt",
                "val foo = \"bar\""
            )

            if (SemVer.parse(ktLintVersion) < SemVer.parse("0.34.2")) {
                buildAndFail(CHECK_PARENT_TASK_NAME) {
                    assertThat(task(":runKtlintCheckOverMainSourceSet")?.outcome)
                        .`as`("Rules disabling is supported since 0.34.2 ktlint version")
                        .isEqualTo(TaskOutcome.FAILED)
                }
            } else if (SemVer.parse(ktLintVersion) < SemVer.parse("0.48.0")) {
                build(CHECK_PARENT_TASK_NAME) {
                    assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                    assertThat(output).doesNotContain("Property 'ktlint_disabled_rules' is deprecated")
                }
            } else {
                build(CHECK_PARENT_TASK_NAME) {
                    assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                    assertThat(output)
                        .`as`("old disabled_rules list is deprecated in 0.48, slated for removal in 0.49")
                        .contains("Property 'ktlint_disabled_rules' is deprecated")
                }
            }
        }
    }

    @DisplayName("Should lint without errors when 'final-newline' rule is disabled via editorconfig")
    @ParameterizedTest(name = "{0} with KtLint {1}: {displayName}")
    @ArgumentsSource(KtLintSupportedVersionsTest.SupportedKtlintVersionsProvider::class)
    fun lintDisabledRuleFinalNewlineEditorconfig(gradleVersion: GradleVersion, ktLintVersion: String) {
        if (SemVer.parse(ktLintVersion) >= SemVer.parse("0.48.0")) {
            // new way of disabling rules introduced in 0.48
            project(gradleVersion) {
                editorConfig.appendText(
                    """
                    root = true

                    [*.kt]
                    ktlint_standard_final-newline = disabled
                    """.trimIndent()
                )
                //language=Groovy
                buildGradle.appendText(
                    """
                    ktlint.version = "$ktLintVersion"
                    """.trimIndent()
                )

                createSourceFile(
                    "src/main/kotlin/CleanSource.kt",
                    "val foo = \"bar\""
                )

                build(CHECK_PARENT_TASK_NAME) {
                    assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                    assertThat(output).doesNotContain("Property 'ktlint_disabled_rules' is deprecated")
                }
            }
        }
    }

    @DisplayName("Should lint without errors when 'final-newline' and 'no-consecutive-blank-lines' are disabled")
    @CommonTest
    fun lintOnMultipleDisabledRules(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.disabledRules = ["final-newline", "no-consecutive-blank-lines"]
                """.trimIndent()
            )

            createSourceFile(
                "src/main/kotlin/CleanSource.kt",
                """
                fun some() {
                    print("Woohoo!")
                }


                val foo = "bar"
                """.trimIndent()
            )

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    @DisplayName("Should lint without errors when 'no-consecutive-blank-lines' are disabled in the code")
    @CommonTest
    fun lintRuleDisabledInTheCode(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            createSourceFile(
                "src/main/kotlin/CleanSource.kt",
                """
                /* ktlint-disable no-consecutive-blank-lines */
                fun some() {
                    print("Woohoo!")
                }


                /* ktlint-enable no-consecutive-blank-lines */

                val foo = "bar"

                """.trimIndent()
            )

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    @DisplayName("Should fail if KtLint version is lower then 0.34.2 and disabled rules configuration is set")
    @CommonTest
    fun lintShouldFailOnUnsupportedVersion(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.version = "0.34.0"
                ktlint.disabledRules = ["final-newline"]
                """.trimIndent()
            )

            withCleanSources()

            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(
                    task(":${KtLintCheckTask.buildTaskNameForSourceSet("main")}")?.outcome
                ).isEqualTo(TaskOutcome.FAILED)
                assertThat(output).contains("Rules disabling is supported since 0.34.2 ktlint version.")
            }
        }
    }
}
