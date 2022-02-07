package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.TestProject
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName

/**
 * Contains all tests related to `.editorconfig` files support.
 */
@GradleTestVersions
class EditorConfigTests : AbstractPluginTest() {
    private val lintTaskName = KtLintCheckTask.buildTaskNameForSourceSet("main")

    @DisplayName("Check task should be UP-TO-DATE if '.editorconfig' content didn't change")
    @CommonTest
    fun checkUpToDateOnSameContent(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            createEditorconfigFile()

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            }
        }
    }

    @DisplayName("Check task should be UP-TO-DATE if additional '.editorconfig' content hasn't changed")
    @CommonTest
    fun checkUpToDateAdditionalContentNotChanged(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val additionalConfigPath = temporaryFolder.resolve("some/additional/folder/").toString()
            withCleanSources()
            createEditorconfigFile(filePath = additionalConfigPath)

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            }
        }
    }

    @DisplayName("Check task should rerun if '.editorconfig' content has changed")
    @CommonTest
    fun checkRerunOnContentChange(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            createEditorconfigFile()

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            modifyEditorconfigFile(10)
            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            }
        }
    }

    @DisplayName("Check task should rerun if additional '.editorconfig' file content has changed")
    @CommonTest
    fun checkRerunOnAdditionalEditorconfigFileChange(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val additionalConfigPath = temporaryFolder.resolve("some/additional/folder").toString()
            withCleanSources()
            createEditorconfigFile(filePath = additionalConfigPath)

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            modifyEditorconfigFile(
                maxLineLength = 10,
                filePath = additionalConfigPath
            )

            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            }
        }
    }

    @DisplayName("Check task should rerun if root '.editorconfig' file content has changed")
    @CommonTest
    fun checkRerunOnRootFileContentChange(gradleVersion: GradleVersion) {
        val projectWithModulesLocation = temporaryFolder.resolve("modularized").also { it.mkdirs() }
        project(gradleVersion, projectPath = projectWithModulesLocation) {
            val moduleLocation = projectWithModulesLocation.resolve("test/module1").also { it.mkdirs() }

            //language=Groovy
            settingsGradle.appendText(
                """
                    
                include ":test:module1"
                """.trimIndent()
            )

            //language=Groovy
            buildGradle.appendText(
                """
    
                allprojects {
                    repositories {
                        mavenCentral()
                    }
                }
                """.trimIndent()
            )
            createEditorconfigFile()

            //language=Groovy
            moduleLocation.buildFile().writeText(
                """
                plugins {
                    id "org.jetbrains.kotlin.jvm"
                    id "org.jlleitschuh.gradle.ktlint"
                }
                """.trimIndent()
            )
            moduleLocation.withCleanSources()

            build(":test:module1:$CHECK_PARENT_TASK_NAME") {
                assertThat(task(":test:module1:$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            modifyEditorconfigFile(10)

            buildAndFail(":test:module1:$CHECK_PARENT_TASK_NAME") {
                assertThat(task(":test:module1:$lintTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(task(":test:module1:$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            }
        }
    }

    private fun TestProject.createEditorconfigFile(
        maxLineLength: Int = 120,
        filePath: String = ""
    ) = createSourceFile(
        "$filePath.editorconfig",
        """
            [*.{kt,kts}]
            max_line_length=$maxLineLength
        """.trimIndent()
    )

    private fun TestProject.modifyEditorconfigFile(
        maxLineLength: Int,
        filePath: String = ""
    ) {
        val editorconfigFile = projectPath.resolve("$filePath.editorconfig")
        if (editorconfigFile.exists()) {
            editorconfigFile.delete()
        }
        createEditorconfigFile(maxLineLength)
    }
}
