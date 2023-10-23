package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.TestProject
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName

@GradleTestVersions
class ReportersTest : AbstractPluginTest() {

    @DisplayName("Should create multiple reports")
    @CommonTest
    fun multipleReports(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.reporters {
                    reporter "checkstyle"
                    reporter "json"
                }
                """.trimIndent()
            )
            withFailingSources()

            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
                assertThat(output).contains("Unnecessary long whitespace")
                assertReportNotCreated(ReporterType.PLAIN.fileExtension, mainSourceSetCheckTaskName)
                assertReportCreated(ReporterType.CHECKSTYLE.fileExtension, mainSourceSetCheckTaskName)
                assertReportCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
            }
        }
    }

    @DisplayName("Should create 3rd party report")
    @CommonTest
    internal fun thirdPartyReport(gradleVersion: GradleVersion) {
        // TODO: switch to some 3rd party reporter that is published to Maven Central
        project(gradleVersion) {
            // https://github.com/mcassiano/ktlint-html-reporter/releases
            //language=Groovy
            buildGradle.appendText(
                """

                repositories {
                    jcenter()
                }

                ktlint.reporters {
                    reporter "checkstyle"
                    customReporters {
                        "html" {
                            fileExtension = "html"
                            dependency = "me.cassiano:ktlint-html-reporter:0.2.3"
                        }
                    }
                }
                """.trimIndent()
            )
            withFailingSources()

            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
                assertThat(output).contains("Unnecessary long whitespace")
                assertReportCreated(ReporterType.CHECKSTYLE.fileExtension, mainSourceSetCheckTaskName)
                assertReportNotCreated(ReporterType.PLAIN.fileExtension, mainSourceSetCheckTaskName)
                assertReportNotCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
                assertReportCreated("html", mainSourceSetCheckTaskName)
            }
        }
    }

    @DisplayName("Task is not UP-TO-DATE when another reporter was enabled in the build script")
    @CommonTest
    fun anotherReporterNotUpToDate(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.reporters {
                    reporter "json"
                    reporter "plain"
                }
                """.trimIndent()
            )

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertReportCreated(ReporterType.PLAIN.fileExtension, mainSourceSetCheckTaskName)
                assertReportCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
            }

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
                assertReportCreated(ReporterType.PLAIN.fileExtension, mainSourceSetCheckTaskName)
                assertReportCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
                assertReportNotCreated(ReporterType.CHECKSTYLE.fileExtension, mainSourceSetCheckTaskName)
            }

            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.reporters {
                    reporter "json"
                    reporter "plain_group_by_file"
                }
                """.trimIndent()
            )

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertReportCreated(ReporterType.PLAIN_GROUP_BY_FILE.fileExtension, mainSourceSetCheckTaskName)
                assertReportCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
                assertReportNotCreated(ReporterType.CHECKSTYLE.fileExtension, mainSourceSetCheckTaskName)
            }

            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.reporters {
                    reporter "json"
                    reporter "checkstyle"
                }
                """.trimIndent()
            )

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertReportCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
                assertReportCreated(ReporterType.CHECKSTYLE.fileExtension, mainSourceSetCheckTaskName)
                // TODO: Stale reports are not cleaned up
                assertReportCreated(ReporterType.PLAIN.fileExtension, mainSourceSetCheckTaskName)
            }
        }
    }

    @DisplayName("Should use plain reporter if no reporters are defined")
    @CommonTest
    fun defaultReporter(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withFailingSources()

            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
                assertReportCreated(ReporterType.PLAIN.fileExtension, mainSourceSetCheckTaskName)
                assertReportNotCreated(ReporterType.CHECKSTYLE.fileExtension, mainSourceSetCheckTaskName)
                assertReportNotCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
            }
        }
    }

    @DisplayName("Should generate html report")
    @CommonTest
    internal fun htmlReport(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.reporters {
                    reporter "html"
                }
                """.trimIndent()
            )

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertReportCreated(ReporterType.HTML.fileExtension, mainSourceSetCheckTaskName)
            }
        }
    }

    @DisplayName("Should generate sarif report")
    @CommonTest
    internal fun sarifReport(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.reporters {
                    reporter "sarif"
                }
                """.trimIndent()
            )

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertReportCreated(ReporterType.SARIF.fileExtension, mainSourceSetCheckTaskName)
            }
        }
    }

    @DisplayName("Should allow to set custom location for generated reports")
    @CommonTest
    internal fun customReportsLocation(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withFailingSources()

            val newLocation = "other/location"
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.reporters {
                    reporter "checkstyle"
                    reporter "json"
                }

                tasks.withType(org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask.class) {
                    reportsOutputDirectory.set(project.layout.buildDirectory.dir("$newLocation/${'$'}name"))
                }
                """.trimIndent()
            )

            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)

                assertReportNotCreated(ReporterType.CHECKSTYLE.fileExtension, mainSourceSetCheckTaskName)
                assertReportCreated(
                    ReporterType.CHECKSTYLE.fileExtension,
                    mainSourceSetCheckTaskName,
                    "build/$newLocation/$mainSourceSetCheckTaskName"
                )

                assertReportNotCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
                assertReportCreated(
                    ReporterType.JSON.fileExtension,
                    mainSourceSetCheckTaskName,
                    "build/$newLocation/$mainSourceSetCheckTaskName"
                )
            }
        }
    }

    private fun TestProject.assertReportCreated(
        reportFileExtension: String,
        taskName: String,
        baseLocation: String = "build/reports/ktlint/$taskName"
    ) {
        assertThat(
            reportLocation(baseLocation, taskName, reportFileExtension).isFile
        ).isTrue
    }

    private fun TestProject.assertReportNotCreated(
        reportFileExtension: String,
        taskName: String,
        baseLocation: String = "build/reports/ktlint/$taskName"
    ) {
        assertThat(
            reportLocation(baseLocation, taskName, reportFileExtension).isFile
        ).isFalse
    }

    private fun TestProject.reportLocation(
        reportsLocation: String,
        taskName: String,
        reportFileExtension: String
    ) = projectPath.resolve(
        "$reportsLocation/$taskName.$reportFileExtension"
    )
}
