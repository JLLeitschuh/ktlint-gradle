package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.logging.Logging
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

@GradleTestVersions
class DisabledRulesTest : AbstractPluginTest() {
    private val logger = Logging.getLogger(DisabledRulesTest::class.java)

    @DisplayName("Should lint without errors when 'final-newline' rule is disabled via editorconfig")
    @ParameterizedTest(name = "{0} with KtLint {1}: {displayName}")
    @ArgumentsSource(KtLintSupportedVersionsTest.SupportedKtlintVersionsProvider::class)
    fun lintDisabledRuleFinalNewlineEditorconfig(gradleVersion: GradleVersion, ktLintVersion: String) {
        project(gradleVersion) {
            editorConfig.appendText(
                """
root = true

[*.kt]
ktlint_standard_final-newline = disabled
"""
            )

            //language=Groovy
            buildGradle.appendText(
                """
ktlint.version = "$ktLintVersion"
"""
            )

            createSourceFile(
                "src/main/kotlin/CleanSource.kt",
                "val foo = \"bar\""
            )

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(output).doesNotContain("Property 'ktlint_disabled_rules' is deprecated")
                assertThat(output).doesNotContain("Property 'disabled_rules' is deprecated")
            }
        }
    }
}
