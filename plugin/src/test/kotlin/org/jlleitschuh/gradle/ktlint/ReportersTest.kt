package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jlleitschuh.gradle.ktlint.KtlintBasePlugin.Companion.LOWEST_SUPPORTED_GRADLE_VERSION
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Runs [ReportersTest] with the current version of Gradle.
 */
class GradleCurrentReportersTests : ReportersTest()

/**
 * Runs [ReportersTest] with lowest supported Gradle version.
 */
@Suppress("ClassName")
class GradleLowestSupportedReportersTest : ReportersTest() {
    override fun gradleRunnerFor(
        vararg arguments: String,
        projectRoot: File
    ): GradleRunner =
        super.gradleRunnerFor(*arguments, projectRoot = projectRoot)
            .withGradleVersion(LOWEST_SUPPORTED_GRADLE_VERSION)
}

abstract class ReportersTest : AbstractPluginTest() {
    @BeforeEach
    fun setupBuild() {
        projectRoot.defaultProjectSetup()
    }

    @Test
    fun `Should create multiple reports`() {
        projectRoot.withFailingSources()

        projectRoot.buildFile().appendText(
            """

            ktlint.reporters {
                reporter "checkstyle"
                reporter "json"
            }
            """.trimIndent()
        )

        buildAndFail(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains("Unnecessary space(s)")
            assertReportNotCreated(ReporterType.PLAIN.fileExtension, mainSourceSetCheckTaskName)
            assertReportCreated(ReporterType.CHECKSTYLE.fileExtension, mainSourceSetCheckTaskName)
            assertReportCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
        }
    }

    @Test
    internal fun `Should create 3rd party report`() {
        projectRoot.withFailingSources()

        // https://github.com/mcassiano/ktlint-html-reporter/releases
        projectRoot.buildFile().appendText(
            """
            
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

        buildAndFail(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains("Unnecessary space(s)")
            assertReportCreated(ReporterType.CHECKSTYLE.fileExtension, mainSourceSetCheckTaskName)
            assertReportNotCreated(ReporterType.PLAIN.fileExtension, mainSourceSetCheckTaskName)
            assertReportNotCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
            assertReportCreated("html", mainSourceSetCheckTaskName)
        }
    }

    @Test
    fun `Is out of date when different report is enabled`() {
        projectRoot.withCleanSources()

        projectRoot.buildFile().appendText(
            """

            ktlint.reporters {
                reporter "json"
                reporter "plain"
            }
            """.trimIndent()
        )

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportCreated(ReporterType.PLAIN.fileExtension, mainSourceSetCheckTaskName)
            assertReportCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
        }

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            assertReportCreated(ReporterType.PLAIN.fileExtension, mainSourceSetCheckTaskName)
            assertReportCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
            assertReportNotCreated(ReporterType.CHECKSTYLE.fileExtension, mainSourceSetCheckTaskName)
        }

        projectRoot.buildFile().appendText(
            """

            ktlint.reporters {
                reporter "json"
                reporter "plain_group_by_file"
            }
            """.trimIndent()
        )

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportCreated(ReporterType.PLAIN_GROUP_BY_FILE.fileExtension, mainSourceSetCheckTaskName)
            assertReportCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
            assertReportNotCreated(ReporterType.CHECKSTYLE.fileExtension, mainSourceSetCheckTaskName)
        }

        projectRoot.buildFile().appendText(
            """

            ktlint.reporters {
                reporter "json"
                reporter "checkstyle"
            }
            """.trimIndent()
        )

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
            assertReportCreated(ReporterType.CHECKSTYLE.fileExtension, mainSourceSetCheckTaskName)
            // TODO: Stale reports are not cleaned up
            assertReportCreated(ReporterType.PLAIN.fileExtension, mainSourceSetCheckTaskName)
        }
    }

    @Test
    internal fun `Should use plain reporter if no reporters were defined`() {
        projectRoot.withFailingSources()

        buildAndFail(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertReportCreated(ReporterType.PLAIN.fileExtension, mainSourceSetCheckTaskName)
            assertReportNotCreated(ReporterType.CHECKSTYLE.fileExtension, mainSourceSetCheckTaskName)
            assertReportNotCreated(ReporterType.JSON.fileExtension, mainSourceSetCheckTaskName)
        }
    }

    @Test
    internal fun `Should generate html report`() {
        projectRoot.withCleanSources()

        projectRoot.buildFile().appendText(
            """

            ktlint.reporters {
                reporter "html"
            }
            """.trimIndent()
        )

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportCreated(ReporterType.HTML.fileExtension, mainSourceSetCheckTaskName)
        }
    }

    @Test
    internal fun `Should ignore html reporter on ktlint version less then 0_36_0`() {
        projectRoot.withCleanSources()

        projectRoot.buildFile().appendText(
            """

            ktlint.version = "0.35.0"
            ktlint.reporters {
                reporter "html"
            }
            """.trimIndent()
        )

        build(CHECK_PARENT_TASK_NAME).apply {
            assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportNotCreated(ReporterType.HTML.fileExtension, mainSourceSetCheckTaskName)
        }
    }

    @Test
    internal fun `Should create reports in modified reports output dir`() {
        projectRoot.withFailingSources()
        val newLocation = "other/location"

        projectRoot.buildFile().appendText(
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

        buildAndFail(CHECK_PARENT_TASK_NAME).apply {
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

    private fun assertReportCreated(
        reportFileExtension: String,
        taskName: String,
        baseLocation: String = "build/reports/ktlint/$taskName"
    ) {
        assertThat(
            reportLocation(baseLocation, taskName, reportFileExtension).isFile
        ).isTrue
    }

    private fun assertReportNotCreated(
        reportFileExtension: String,
        taskName: String,
        baseLocation: String = "build/reports/ktlint/$taskName"
    ) {
        assertThat(
            reportLocation(baseLocation, taskName, reportFileExtension).isFile
        ).isFalse
    }

    private fun reportLocation(
        reportsLocation: String,
        taskName: String,
        reportFileExtension: String
    ) = projectRoot.resolve(
        "$reportsLocation/$taskName.$reportFileExtension"
    )
}
