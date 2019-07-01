package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Runs [ReportersTest] with the current version of Gradle.
 */
class GradleCurrentReportersTests : ReportersTest()

/**
 * Runs [ReportersTest] with lowest supported Gradle version.
 */
@Suppress("ClassName")
class Gradle4_10ReportersTest : ReportersTest() {
    override fun gradleRunnerFor(vararg arguments: String): GradleRunner =
        super.gradleRunnerFor(*arguments).withGradleVersion("4.10")
}

abstract class ReportersTest : AbstractPluginTest() {
    @BeforeEach
    fun setupBuild() {
        projectRoot.defaultProjectSetup()
    }

    @Test
    fun `Should create multiple reports`() {
        projectRoot.withFailingSources()

        projectRoot.buildFile().appendText("""

            ktlint.reporters = [ReporterType.PLAIN_GROUP_BY_FILE, ReporterType.CHECKSTYLE, ReporterType.JSON]
        """.trimIndent())

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains("Unnecessary space(s)")
            assertReportCreated(ReporterType.PLAIN_GROUP_BY_FILE.fileExtension)
            assertReportCreated(ReporterType.CHECKSTYLE.fileExtension)
            assertReportCreated(ReporterType.JSON.fileExtension)
        }
    }

    @Test
    internal fun `Should create 3rd party report`() {
        projectRoot.withFailingSources()

        // https://github.com/mcassiano/ktlint-html-reporter/releases
        projectRoot.buildFile().appendText("""
            
            ktlint.customReporters {
                reporter "html", "html", "me.cassiano:ktlint-html-reporter:0.2.3"
            }
        """.trimIndent())

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains("Unnecessary space(s)")
            assertReportCreated(ReporterType.PLAIN.fileExtension)
            assertReportCreated(ReporterType.CHECKSTYLE.fileExtension)
            assertReportNotCreated(ReporterType.JSON.fileExtension)
            assertReportCreated("html")
        }
    }

    @Test
    fun `Is out of date when different report is enabled`() {
        projectRoot.withCleanSources()

        projectRoot.buildFile().appendText("""

            ktlint.reporters = [ReporterType.JSON, ReporterType.PLAIN]
        """.trimIndent())

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportCreated(ReporterType.PLAIN.fileExtension)
            assertReportCreated(ReporterType.JSON.fileExtension)
        }

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            assertReportCreated(ReporterType.PLAIN.fileExtension)
            assertReportCreated(ReporterType.JSON.fileExtension)
            assertReportNotCreated(ReporterType.CHECKSTYLE.fileExtension)
        }

        projectRoot.buildFile().appendText("""

            ktlint.reporters = [ReporterType.JSON, ReporterType.PLAIN_GROUP_BY_FILE]
        """.trimIndent())

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportCreated(ReporterType.PLAIN_GROUP_BY_FILE.fileExtension)
            assertReportCreated(ReporterType.JSON.fileExtension)
        }

        projectRoot.buildFile().appendText("""

            ktlint.reporters = [ReporterType.JSON, ReporterType.CHECKSTYLE]
        """.trimIndent())

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportCreated(ReporterType.JSON.fileExtension)
            assertReportCreated(ReporterType.CHECKSTYLE.fileExtension)
            // TODO: Stale reports are not cleaned up
            assertReportCreated(ReporterType.PLAIN.fileExtension)
        }
    }

    private fun assertReportCreated(reportFileExtension: String) {
        assertThat(reportLocation(reportFileExtension).isFile).isTrue()
    }

    private fun assertReportNotCreated(reportFileExtension: String) {
        assertThat(reportLocation(reportFileExtension).isFile).isFalse()
    }

    private fun reportLocation(reportFileExtension: String) =
        projectRoot.resolve("build/reports/ktlint/ktlintMainSourceSetCheck.$reportFileExtension")
}
