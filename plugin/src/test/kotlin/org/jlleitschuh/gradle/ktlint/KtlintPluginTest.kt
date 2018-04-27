package org.jlleitschuh.gradle.ktlint

import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KtlintPluginTest : AbstractPluginTest() {

    @Before
    fun setupBuild() {
        projectRoot.apply {
            resolve("build.gradle").writeText("""
                ${buildscriptBlockWithUnderTestPlugin()}

                ${pluginsBlockWithKotlinJvmPlugin()}

                apply plugin: "org.jlleitschuh.gradle.ktlint"

                repositories {
                    gradlePluginPortal()
                }

                ktlint.reporters = ["PLAIN", "CHECKSTYLE"]
            """.trimIndent())
        }
    }

    @Test
    fun `should fail check on failing sources`() {

        withFailingSources()

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.FAILED))
            assertThat(output, containsString("Unnecessary space(s)"))
            assertReportCreated(ReporterType.PLAIN)
            assertReportCreated(ReporterType.CHECKSTYLE)
            assertReportNotCreated(ReporterType.JSON)
        }
    }

    @Test
    fun `creates multiple reports`() {

        withFailingSources()

        projectRoot.resolve("build.gradle").appendText("""

            ktlint.reporters = ["PLAIN_GROUP_BY_FILE", "CHECKSTYLE", "JSON"]
        """.trimIndent())

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.FAILED))
            assertThat(output, containsString("Unnecessary space(s)"))
            assertReportCreated(ReporterType.PLAIN_GROUP_BY_FILE)
            assertReportCreated(ReporterType.CHECKSTYLE)
            assertReportCreated(ReporterType.JSON)
        }
    }

    private
    fun assertReportCreated(reportType: ReporterType) {
        assertTrue(reportLocation(reportType).isFile)
    }

    private
    fun assertReportNotCreated(reportType: ReporterType) {
        assertFalse(reportLocation(reportType).isFile)
    }

    private fun reportLocation(reportType: ReporterType) =
            projectRoot.resolve("build/reports/ktlint/ktlint-main.${reportType.fileExtension}")

    @Test
    fun `should succeed check on clean sources`() {

        withCleanSources()

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.SUCCESS))
        }
    }
}
