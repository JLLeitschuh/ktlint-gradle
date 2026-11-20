package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.TestProject
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName

@GradleTestVersions
class IncrementalSourcesTest : AbstractPluginTest() {
    @DisplayName("Check should discard errors when deleting a source is the only change")
    @CommonTest
    fun checkAfterDeletingFailingSource(gradleVersion: GradleVersion) {
        verifyDeletedSourceErrors(gradleVersion, mainSourceSetCheckTaskName)
    }

    @DisplayName("Format should discard errors when deleting a source is the only change")
    @CommonTest
    fun formatAfterDeletingFailingSource(gradleVersion: GradleVersion) {
        verifyDeletedSourceErrors(gradleVersion, mainSourceSetFormatTaskName)
    }

    @DisplayName("Check should preserve errors in remaining sources after a deletion")
    @CommonTest
    fun checkPreservesRemainingErrors(gradleVersion: GradleVersion) {
        verifyDeletedSourceErrors(gradleVersion, mainSourceSetCheckTaskName, remainingErrors = true)
    }

    @DisplayName("Format should preserve errors in remaining sources after a deletion")
    @CommonTest
    fun formatPreservesRemainingErrors(gradleVersion: GradleVersion) {
        verifyDeletedSourceErrors(gradleVersion, mainSourceSetFormatTaskName, remainingErrors = true)
    }

    private fun verifyDeletedSourceErrors(
        gradleVersion: GradleVersion,
        taskName: String,
        remainingErrors: Boolean = false
    ) {
        project(gradleVersion) {
            withCleanSources()
            editorConfig.writeText(
                """
                root = true

                [*.{kt,kts}]
                max_line_length = 60
                """.trimIndent()
            )
            val deletedSource = "src/main/kotlin/DeletedSource.kt"
            withUnfixableSource(deletedSource)
            if (remainingErrors) {
                withUnfixableSource("src/main/kotlin/RemainingSource.kt")
            }

            val originalContents = projectPath.resolve(deletedSource).readText()
            buildAndFail(taskName, "--no-build-cache") {
                assertThat(task(":$taskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
                assertThat(output).contains("DeletedSource.kt", "Exceeded max line length")
            }
            assertThat(projectPath.resolve(deletedSource).readText()).isEqualTo(originalContents)

            removeSourceFile(deletedSource)

            if (remainingErrors) {
                buildAndFail(taskName, "--no-build-cache") {
                    assertThat(task(":$taskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
                    assertThat(output).contains("RemainingSource.kt", "Exceeded max line length")
                    assertThat(output).doesNotContain("DeletedSource.kt")
                }
            } else {
                build(taskName, "--no-build-cache") {
                    assertThat(task(":$taskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                    assertThat(output).doesNotContain("DeletedSource.kt")
                }
                build(taskName, "--no-build-cache") {
                    assertThat(task(":$taskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
                }
            }
        }
    }

    private fun TestProject.withUnfixableSource(path: String) {
        createSourceFile(
            path,
            """
            |val aVeryLongVariableNameThatExceedsTheConfiguredLineLengthAndCannotBeShortened =
            |    1
            |
            """.trimMargin()
        )
    }
}
