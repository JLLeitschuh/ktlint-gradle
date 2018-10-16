package org.jlleitschuh.gradle.ktlint

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers.*
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Runs the tests with the current version of Gradle.
 */
class GradleCurrentKtlintPluginTest : BaseKtlintPluginTest()

@Suppress("ClassName")
class Gradle4_8KtlintPluginTest : BaseKtlintPluginTest() {

    override fun gradleRunnerFor(vararg arguments: String): GradleRunner =
            super.gradleRunnerFor(*arguments).withGradleVersion("4.8")
}

abstract class BaseKtlintPluginTest : AbstractPluginTest() {

    @Before
    fun setupBuild() {
        projectRoot.apply {
            buildFile().writeText("""
                ${buildscriptBlockWithUnderTestPlugin()}

                ${pluginsBlockWithKotlinJvmPlugin()}

                apply plugin: "org.jlleitschuh.gradle.ktlint"

                repositories {
                    gradlePluginPortal()
                }

                import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

                ktlint.reporters = [ReporterType.CHECKSTYLE, ReporterType.PLAIN]
            """.trimIndent())
        }
    }

    @Test
    fun `fails on versions older than 0_10`() {
        projectRoot.buildFile().appendText("""

            ktlint.version = "0.9.0"
        """.trimIndent())

        projectRoot.withCleanSources()

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")?.outcome, equalTo(TaskOutcome.FAILED))
            assertThat(output, containsString("Ktlint versions less than 0.10.0 are not supported. Detected Ktlint version: 0.9.0."))
        }
    }

    @Test
    fun `should fail check on failing sources`() {
        projectRoot.withFailingSources()

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
        projectRoot.withFailingSources()

        projectRoot.buildFile().appendText("""

            ktlint.reporters = [ReporterType.PLAIN_GROUP_BY_FILE, ReporterType.CHECKSTYLE, ReporterType.JSON]
        """.trimIndent())

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.FAILED))
            assertThat(output, containsString("Unnecessary space(s)"))
            assertReportCreated(ReporterType.PLAIN_GROUP_BY_FILE)
            assertReportCreated(ReporterType.CHECKSTYLE)
            assertReportCreated(ReporterType.JSON)
        }
    }

    @Test
    fun `is out of date when different report is enabled`() {
        projectRoot.withCleanSources()

        projectRoot.buildFile().appendText("""

            ktlint.reporters = [ReporterType.JSON, ReporterType.PLAIN]
        """.trimIndent())

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.SUCCESS))
            assertReportCreated(ReporterType.PLAIN)
            assertReportCreated(ReporterType.JSON)
        }

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.UP_TO_DATE))
            assertReportCreated(ReporterType.PLAIN)
            assertReportCreated(ReporterType.JSON)
            assertReportNotCreated(ReporterType.CHECKSTYLE)
        }

        projectRoot.buildFile().appendText("""

            ktlint.reporters = [ReporterType.JSON, ReporterType.PLAIN_GROUP_BY_FILE]
        """.trimIndent())

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.SUCCESS))
            assertReportCreated(ReporterType.PLAIN_GROUP_BY_FILE)
            assertReportCreated(ReporterType.JSON)
        }

        projectRoot.buildFile().appendText("""

            ktlint.reporters = [ReporterType.JSON, ReporterType.CHECKSTYLE]
        """.trimIndent())

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.SUCCESS))
            assertReportCreated(ReporterType.JSON)
            assertReportCreated(ReporterType.CHECKSTYLE)
            // TODO: Stale reports are not cleaned up
            assertReportCreated(ReporterType.PLAIN)
        }
    }

    @Test
    fun `Check task should be up_to_date if editorconfig content not changed`() {
        projectRoot.withCleanSources()
        projectRoot.createEditorconfigFile()

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.SUCCESS))
        }
        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.UP_TO_DATE))
        }
    }

    @Test
    fun `Check task should rerun if editorconfig content changed`() {
        projectRoot.withCleanSources()
        projectRoot.createEditorconfigFile()

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.SUCCESS))
        }

        projectRoot.modifyEditorconfigFile(100)
        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.SUCCESS))
        }
    }

    @Test
    fun `check task is relocatable`() {
        val originalLocation = temporaryFolder.root.resolve("original")
        val relocatedLocation = temporaryFolder.root.resolve("relocated")
        val localBuildCacheDirectory = temporaryFolder.root.resolve("build-cache")
        listOf(originalLocation, relocatedLocation).forEach {
            it.apply {
                withCleanSources()
                buildFile().writeText("""
                    ${buildscriptBlockWithUnderTestPlugin()}

                    ${pluginsBlockWithKotlinJvmPlugin()}

                    apply plugin: "org.jlleitschuh.gradle.ktlint"

                    repositories {
                        gradlePluginPortal()
                    }

                    import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

                    ktlint.reporters = [ReporterType.PLAIN, ReporterType.CHECKSTYLE]
                """.trimIndent())
                settingsFile().writeText("""
                    buildCache {
                        local {
                            directory = '${localBuildCacheDirectory.toURI()}'
                        }
                    }
                """.trimIndent())
            }
        }

        GradleRunner.create()
                .withProjectDir(originalLocation)
                .withArguments("ktlintCheck", "--build-cache")
                .forwardOutput()
                .build().apply {
                    assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.SUCCESS))
                }

        GradleRunner.create()
                .withProjectDir(relocatedLocation)
                .withArguments("ktlintCheck", "--build-cache")
                .forwardOutput()
                .build().apply {
                    assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.FROM_CACHE))
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
            projectRoot.resolve("build/reports/ktlint/ktlintMainCheck.${reportType.fileExtension}")

    @Test
    fun `should succeed check on clean sources`() {

        projectRoot.withCleanSources()

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.SUCCESS))
        }
    }

    @Test
    fun `should generate code style files in project`() {
        projectRoot.withCleanSources()
        val ideaRootDir = projectRoot.resolve(".idea").apply { mkdir() }

        build("ktlintApplyToIdea").apply {
            assertThat(task(":ktlintApplyToIdea")?.outcome, equalTo(TaskOutcome.SUCCESS))
            assertThat(ideaRootDir.listFiles().isNotEmpty(), equalTo(true))
        }
    }

    @Test
    fun `should fail ktlintApplyToIdea if ktlint version less then 0_22_0`() {
        projectRoot.withCleanSources()
        projectRoot.buildFile().appendText("""

            ktlint.version = "0.10.0"
        """.trimIndent())

        buildAndFail(":ktlintApplyToIdea").apply {
            assertThat(task(":ktlintApplyToIdea")?.outcome, equalTo(TaskOutcome.FAILED))
            assertThat(output, containsString("Apply per project in only available from ktlint 0.22.0"))
        }
    }

    @Test
    fun `should generate code style file globally`() {
        val ideaRootDir = projectRoot.resolve(".idea").apply { mkdir() }

        build(":ktlintApplyToIdeaGlobally").apply {
            assertThat(task(":ktlintApplyToIdeaGlobally")?.outcome, equalTo(TaskOutcome.SUCCESS))
            assertThat(ideaRootDir.listFiles().isNotEmpty(), equalTo(true))
        }
    }

    @Test
    fun `should show only plugin meta tasks in task output`() {
        projectRoot.withCleanSources()

        build("tasks").apply {
            val ktlintTasks = output
                    .lineSequence()
                    .filter { it.startsWith("ktlint") }
                    .toList()

            assertThat(ktlintTasks.size, equalTo(4))
            assertThat(ktlintTasks, hasItems(
                    startsWith(CHECK_PARENT_TASK_NAME),
                    startsWith(FORMAT_PARENT_TASK_NAME),
                    startsWith(APPLY_TO_IDEA_TASK_NAME),
                    startsWith(APPLY_TO_IDEA_GLOBALLY_TASK_NAME)
            ))
        }
    }

    @Test
    fun `should show all ktlint tasks in task output`() {
        build("tasks", "--all").apply {
            val ktlintTasks = output
                    .lineSequence()
                    .filter { it.startsWith("ktlint") }
                    .toList()

            // Plus for main and test sources format and check tasks
            assertThat(ktlintTasks.size, equalTo(8))
            assertThat(ktlintTasks, hasItems(
                    startsWith(CHECK_PARENT_TASK_NAME),
                    startsWith(FORMAT_PARENT_TASK_NAME),
                    startsWith(APPLY_TO_IDEA_TASK_NAME),
                    startsWith(APPLY_TO_IDEA_GLOBALLY_TASK_NAME)
            ))
        }
    }

    @Test
    fun `Should ignore excluded sources`() {
        projectRoot.withCleanSources()
        projectRoot.withFailingSources()

        projectRoot.buildFile().appendText("""

            ktlint.filter { exclude("**/fail-source.kt") }
        """.trimIndent())

        build(":ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.SUCCESS))
        }
    }
}
