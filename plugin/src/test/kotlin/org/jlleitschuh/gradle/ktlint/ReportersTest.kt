package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
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

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains("Unnecessary space(s)")
            assertReportNotCreated(ReporterType.PLAIN.fileExtension)
            assertReportCreated(ReporterType.CHECKSTYLE.fileExtension)
            assertReportCreated(ReporterType.JSON.fileExtension)
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

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains("Unnecessary space(s)")
            assertReportCreated(ReporterType.CHECKSTYLE.fileExtension)
            assertReportNotCreated(ReporterType.PLAIN.fileExtension)
            assertReportNotCreated(ReporterType.JSON.fileExtension)
            assertReportCreated("html")
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

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportCreated(ReporterType.PLAIN.fileExtension)
            assertReportCreated(ReporterType.JSON.fileExtension)
        }

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            assertReportCreated(ReporterType.PLAIN.fileExtension)
            assertReportCreated(ReporterType.JSON.fileExtension)
            assertReportNotCreated(ReporterType.CHECKSTYLE.fileExtension)
        }

        projectRoot.buildFile().appendText(
            """

            ktlint.reporters {
                reporter "json"
                reporter "plain_group_by_file"
            }
            """.trimIndent()
        )

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportCreated(ReporterType.PLAIN_GROUP_BY_FILE.fileExtension)
            assertReportCreated(ReporterType.JSON.fileExtension)
        }

        projectRoot.buildFile().appendText(
            """

            ktlint.reporters {
                reporter "json"
                reporter "checkstyle"
            }
            """.trimIndent()
        )

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportCreated(ReporterType.JSON.fileExtension)
            assertReportCreated(ReporterType.CHECKSTYLE.fileExtension)
            // TODO: Stale reports are not cleaned up
            assertReportCreated(ReporterType.PLAIN.fileExtension)
        }
    }

    @Test
    internal fun `Should use plain reporter if no reporters were defined`() {
        projectRoot.withFailingSources()

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertReportCreated(ReporterType.PLAIN.fileExtension)
            assertReportNotCreated(ReporterType.CHECKSTYLE.fileExtension)
            assertReportNotCreated(ReporterType.JSON.fileExtension)
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

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportCreated(ReporterType.HTML.fileExtension)
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

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportNotCreated(ReporterType.HTML.fileExtension)
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

            tasks.withType(org.jlleitschuh.gradle.ktlint.KtlintCheckTask.class) {
                reporterOutputDir = project.layout.buildDirectory.dir("$newLocation")
            }
            """.trimIndent()
        )

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.FAILED)

            assertReportNotCreated(ReporterType.CHECKSTYLE.fileExtension)
            assertReportCreated(ReporterType.CHECKSTYLE.fileExtension, "build/$newLocation")

            assertReportNotCreated(ReporterType.JSON.fileExtension)
            assertReportCreated(ReporterType.JSON.fileExtension, "build/$newLocation")
        }
    }

    private fun assertReportCreated(
        reportFileExtension: String,
        reportsLocation: String = "build/reports/ktlint"
    ) {
        assertThat(reportLocation(reportsLocation, reportFileExtension).isFile).isTrue()
    }

    private fun assertReportNotCreated(
        reportFileExtension: String,
        reportsLocation: String = "build/reports/ktlint"
    ) {
        assertThat(reportLocation(reportsLocation, reportFileExtension).isFile).isFalse()
    }

    private fun reportLocation(
        reportsLocation: String,
        reportFileExtension: String
    ) = projectRoot.resolve("$reportsLocation/ktlintMainSourceSetCheck.$reportFileExtension")
}
