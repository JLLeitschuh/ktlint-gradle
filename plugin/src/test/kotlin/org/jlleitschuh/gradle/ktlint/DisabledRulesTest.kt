package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName

@GradleTestVersions
class DisabledRulesTest : AbstractPluginTest() {

    @DisplayName("Should lint without errors when 'final-newline' rule is disabled")
    @CommonTest
    fun lintDisabledRuleFinalNewline(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.disabledRules = ["final-newline"]
                """.trimIndent()
            )

            createSourceFile(
                "src/main/kotlin/CleanSource.kt",
                """
                val foo = "bar"
                """.trimIndent()
            )

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    @DisplayName("Should lint without errors when 'final-newline', 'no-empty-first-line-in-method-block', and 'no-consecutive-blank-lines' are disabled")
    @CommonTest
    fun lintOnMultipleDisabledRules(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.disabledRules = ["final-newline", "no-empty-first-line-in-method-block", "no-consecutive-blank-lines"]
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

    @DisplayName("Should lint without errors when 'no-empty-first-line-in-method-block' and 'no-consecutive-blank-lines' are disabled in the code")
    @CommonTest
    fun lintRuleDisabledInTheCode(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            createSourceFile(
                "src/main/kotlin/CleanSource.kt",
                """
                /* ktlint-disable no-empty-first-line-in-method-block no-consecutive-blank-lines */
                fun some() {


                    print("Woohoo!")
                }
                /* ktlint-enable no-empty-first-line-in-method-block no-consecutive-blank-lines */

                val foo = "bar"

                """.trimIndent()
            )

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }
}
