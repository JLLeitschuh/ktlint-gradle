package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.tasks.GenerateBaselineTask
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.TestProject.Companion.FAIL_SOURCE_FILE
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName
import java.io.File

@GradleTestVersions
class KtlintBaselineSupportTest : AbstractPluginTest() {

    @DisplayName("Should generate empty baseline file on style violations")
    @CommonTest
    internal fun emptyBaseline(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()

            build(GenerateBaselineTask.NAME) {
                assertThat(task(":${GenerateBaselineTask.NAME}")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                val baselineFile = projectPath.defaultBaselineFile
                assertThat(baselineFile).exists()
                assertThat(baselineFile.readText()).isEqualToNormalizingNewlines(
                    """
                    <?xml version="1.0" encoding="utf-8"?>
                    <baseline version="1.0">
                    </baseline>

                    """.trimIndent()
                )
            }
        }
    }

    @DisplayName("Should generate baseline with found style violations")
    @CommonTest
    fun generateBaseline(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withFailingSources()
            withFailingKotlinScript()

            build(GenerateBaselineTask.NAME) {
                assertThat(task(":${GenerateBaselineTask.NAME}")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                val baselineFile = projectPath.defaultBaselineFile
                assertThat(baselineFile).exists()
                //language=xml
                assertThat(baselineFile.readText()).isEqualToNormalizingNewlines(
                    """
                    |<?xml version="1.0" encoding="utf-8"?>
                    |<baseline version="1.0">
                    |    <file name="kotlin-script-fail.kts">
                    |        <error line="1" column="15" source="no-trailing-spaces" />
                    |        <error line="1" column="16" source="no-multi-spaces" />
                    |    </file>
                    |    <file name="src/main/kotlin/FailSource.kt">
                    |        <error line="1" column="5" source="no-multi-spaces" />
                    |        <error line="1" column="10" source="no-multi-spaces" />
                    |        <error line="1" column="15" source="no-multi-spaces" />
                    |    </file>
                    |</baseline>
                    |
                    """.trimMargin()
                )
            }
        }
    }

    @DisplayName("Should overwrite existing baseline file")
    @CommonTest
    fun overwriteBaselineFile(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withFailingSources()

            build(GenerateBaselineTask.NAME)

            removeSourceFile(FAIL_SOURCE_FILE)

            build(GenerateBaselineTask.NAME) {
                assertThat(task(":${GenerateBaselineTask.NAME}")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                val baselineFile = projectPath.defaultBaselineFile
                assertThat(baselineFile).exists()
                assertThat(baselineFile.readText()).isEqualToNormalizingNewlines(
                    """
                    <?xml version="1.0" encoding="utf-8"?>
                    <baseline version="1.0">
                    </baseline>

                    """.trimIndent()
                )
            }
        }
    }

    @DisplayName("Should consider existing issues in baseline")
    @CommonTest
    fun existingIssueFilteredByBaseline(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withFailingSources()

            build(GenerateBaselineTask.NAME)
            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    @DisplayName("Check task should still fail on file style violation that is not present in the baseline")
    @CommonTest
    fun failOnNewStyleViolation(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withFailingSources()
            build(GenerateBaselineTask.NAME)

            withFailingKotlinScript()
            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$kotlinScriptCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            }
        }
    }

    private val File.defaultBaselineFile get() = resolve("config")
        .resolve("ktlint")
        .resolve("baseline.xml")
}
