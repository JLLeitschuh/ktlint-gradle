package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.jlleitschuh.gradle.ktlint.testdsl.projectSetup
import org.junit.jupiter.api.DisplayName
import java.io.File

/**
 * Contains all tests related to "org.jetbrains.kotlin.js" plugin support.
 */
@GradleTestVersions
class KotlinJsPluginTests : AbstractPluginTest() {
    private fun jsProjectSetup(): (File) -> Unit = {
        projectSetup("js").invoke(it)

        //language=Groovy
        it.resolve("build.gradle").appendText(
            """

            kotlin {
                js(IR) {
                    nodejs()
                }
            }
            """.trimIndent()
        )
    }

    @DisplayName("Should add check tasks")
    @CommonTest
    fun addCheckTasks(gradleVersion: GradleVersion) {
        project(gradleVersion, projectSetup = jsProjectSetup()) {
            build("-m", CHECK_PARENT_TASK_NAME) {
                val ktlintTasks = output.lineSequence().toList()

                assertThat(ktlintTasks).anySatisfy {
                    assertThat(it).contains(mainSourceSetCheckTaskName)
                }
                assertThat(ktlintTasks).anySatisfy {
                    assertThat(it).contains(
                        GenerateReportsTask.generateNameForSourceSets(
                            "test",
                            GenerateReportsTask.LintType.CHECK
                        )
                    )
                }
            }
        }
    }

    @DisplayName("Should add format tasks")
    @CommonTest
    fun addFormatTasks(gradleVersion: GradleVersion) {
        project(gradleVersion, projectSetup = jsProjectSetup()) {
            build("-m", FORMAT_PARENT_TASK_NAME) {
                val ktlintTasks = output.lineSequence().toList()

                assertThat(ktlintTasks).anySatisfy {
                    assertThat(it).contains(mainSourceSetFormatTaskName)
                }
                assertThat(ktlintTasks).anySatisfy {
                    assertThat(it).contains(
                        GenerateReportsTask.generateNameForSourceSets(
                            "test",
                            GenerateReportsTask.LintType.FORMAT
                        )
                    )
                }
            }
        }
    }

    @DisplayName("Should fail check task on un-formatted sources")
    @CommonTest
    fun failOnStyleViolation(gradleVersion: GradleVersion) {
        project(gradleVersion, projectSetup = jsProjectSetup()) {
            withFailingSources()

            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            }
        }
    }
}
