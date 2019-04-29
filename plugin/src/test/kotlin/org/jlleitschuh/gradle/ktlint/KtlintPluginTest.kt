package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Runs the tests with the current version of Gradle.
 */
class GradleCurrentKtlintPluginTest : BaseKtlintPluginTest()

@Suppress("ClassName")
class Gradle4_10KtlintPluginTest : BaseKtlintPluginTest() {

    override fun gradleRunnerFor(vararg arguments: String): GradleRunner =
            super.gradleRunnerFor(*arguments).withGradleVersion("4.10")
}

abstract class BaseKtlintPluginTest : AbstractPluginTest() {

    @BeforeEach
    fun setupBuild() {
        projectRoot.apply {
            buildFile().writeText("""
                ${pluginsBlockWithMainPluginAndKotlinJvm()}

                repositories {
                    gradlePluginPortal()
                }

                import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

                ktlint.reporters = [ReporterType.CHECKSTYLE, ReporterType.PLAIN]
            """.trimIndent())
        }
    }

    @Test
    fun `fails on versions older than 0_22_0`() {
        projectRoot.buildFile().appendText("""

            ktlint.version = "0.21.0"
        """.trimIndent())

        projectRoot.withCleanSources()

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output)
                .contains("Ktlint versions less than 0.22.0 are not supported. Detected Ktlint version: 0.21.0.")
        }
    }

    @Test
    fun `should fail check on failing sources`() {
        projectRoot.withFailingSources()

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains("Unnecessary space(s)")
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
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains("Unnecessary space(s)")
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
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportCreated(ReporterType.PLAIN)
            assertReportCreated(ReporterType.JSON)
        }

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            assertReportCreated(ReporterType.PLAIN)
            assertReportCreated(ReporterType.JSON)
            assertReportNotCreated(ReporterType.CHECKSTYLE)
        }

        projectRoot.buildFile().appendText("""

            ktlint.reporters = [ReporterType.JSON, ReporterType.PLAIN_GROUP_BY_FILE]
        """.trimIndent())

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertReportCreated(ReporterType.PLAIN_GROUP_BY_FILE)
            assertReportCreated(ReporterType.JSON)
        }

        projectRoot.buildFile().appendText("""

            ktlint.reporters = [ReporterType.JSON, ReporterType.CHECKSTYLE]
        """.trimIndent())

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
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
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }
    }

    @Test
    fun `Check task should rerun if editorconfig content changed`() {
        projectRoot.withCleanSources()
        projectRoot.createEditorconfigFile()

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        projectRoot.modifyEditorconfigFile(100)
        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Check task should rerun if root editorconfig content changed`() {
        val projectWithModulesLocation = temporaryFolder.resolve("modularized")
        projectWithModulesLocation.mkdirs()
        val moduleLocation = projectWithModulesLocation.resolve("test/module1")
        moduleLocation.mkdirs()

        projectWithModulesLocation.settingsFile().writeText("""
            include ":test:module1"
        """.trimIndent())
        projectWithModulesLocation.buildFile().writeText("""
            ${pluginsBlockWithMainPluginAndKotlinJvm()}

            repositories {
                gradlePluginPortal()
            }

            allprojects {
                repositories {
                    jcenter()
                }
            }
        """.trimIndent())
        projectWithModulesLocation.createEditorconfigFile()
        moduleLocation.buildFile().writeText("""
            apply plugin: "kotlin"
            apply plugin: "org.jlleitschuh.gradle.ktlint"
        """.trimIndent())
        moduleLocation.withCleanSources()

        val gradleRunner = GradleRunner.create()
            .withProjectDir(projectWithModulesLocation)
            .withPluginClasspath()

        gradleRunner
            .withArguments(":test:module1:ktlintCheck")
            .forwardOutput()
            .build().apply {
                assertThat(task(":test:module1:ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

        projectWithModulesLocation.modifyEditorconfigFile(100)

        gradleRunner
            .withArguments(":test:module1:ktlintCheck")
            .forwardOutput()
            .build().apply {
                assertThat(task(":test:module1:ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
    }

    @Test
    fun `check task is relocatable`() {
        val originalLocation = temporaryFolder.resolve("original")
        val relocatedLocation = temporaryFolder.resolve("relocated")
        val localBuildCacheDirectory = temporaryFolder.resolve("build-cache")
        listOf(originalLocation, relocatedLocation).forEach {
            it.apply {
                withCleanSources()
                buildFile().writeText("""
                    ${pluginsBlockWithMainPluginAndKotlinJvm()}

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
            .withPluginClasspath()
            .withArguments("ktlintCheck", "--build-cache")
            .forwardOutput()
            .build().apply {
                assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

        GradleRunner.create()
            .withProjectDir(relocatedLocation)
            .withPluginClasspath()
            .withArguments("ktlintCheck", "--build-cache")
            .forwardOutput()
            .build().apply {
                assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.FROM_CACHE)
            }
    }

    private fun assertReportCreated(reportType: ReporterType) {
        assertThat(reportLocation(reportType).isFile).isTrue()
    }

    private fun assertReportNotCreated(reportType: ReporterType) {
        assertThat(reportLocation(reportType).isFile).isFalse()
    }

    private fun reportLocation(reportType: ReporterType) =
            projectRoot.resolve("build/reports/ktlint/ktlintMainSourceSetCheck.${reportType.fileExtension}")

    @Test
    fun `should succeed check on clean sources`() {

        projectRoot.withCleanSources()

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    fun `should generate code style files in project`() {
        projectRoot.withCleanSources()
        val ideaRootDir = projectRoot.resolve(".idea").apply { mkdir() }

        build("ktlintApplyToIdea").apply {
            assertThat(task(":ktlintApplyToIdea")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(ideaRootDir.listFiles().isNotEmpty()).isTrue()
        }
    }

    @Test
    fun `should generate code style file globally`() {
        val ideaRootDir = projectRoot.resolve(".idea").apply { mkdir() }

        build(":ktlintApplyToIdeaGlobally").apply {
            assertThat(task(":ktlintApplyToIdeaGlobally")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            assertThat(ideaRootDir.listFiles().isNotEmpty()).isTrue()
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

            assertThat(ktlintTasks).hasSize(4)
            assertThat(ktlintTasks).anyMatch { it.startsWith(CHECK_PARENT_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(FORMAT_PARENT_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(APPLY_TO_IDEA_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(APPLY_TO_IDEA_GLOBALLY_TASK_NAME) }
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
            // Plus two kotlin script tasks
            assertThat(ktlintTasks).hasSize(10)
            assertThat(ktlintTasks).anyMatch { it.startsWith(CHECK_PARENT_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(FORMAT_PARENT_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(APPLY_TO_IDEA_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(APPLY_TO_IDEA_GLOBALLY_TASK_NAME) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(KOTLIN_SCRIPT_CHECK_TASK) }
            assertThat(ktlintTasks).anyMatch { it.startsWith(KOTLIN_SCRIPT_FORMAT_TASK) }
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
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    fun `Should fail on additional source set directories files style violation`() {
        projectRoot.withCleanSources()
        val alternativeDirectory = "src/main/shared"
        projectRoot.withAlternativeFailingSources(alternativeDirectory)

        projectRoot.buildFile().appendText("""

            sourceSets {
                findByName("main")?.java?.srcDirs(project.file("$alternativeDirectory"))
            }
        """.trimIndent())

        buildAndFail(":ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome).isEqualTo(TaskOutcome.FAILED)
        }
    }

    @Test
    fun `Should always format again restored to pre-format state sources`() {
        projectRoot.withFailingSources()
        build(":ktlintFormat").apply {
            assertThat(task(":ktlintMainSourceSetFormat")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }

        projectRoot.restoreFailingSources()

        build(":ktlintFormat").apply {
            assertThat(task(":ktlintMainSourceSetFormat")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    fun `Format task should be up-to-date on 3rd run`() {
        projectRoot.withFailingSources()

        build(":ktlintFormat").apply {
            assertThat(task(":ktlintMainSourceSetFormat")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
        build(":ktlintFormat").apply {
            assertThat(task(":ktlintMainSourceSetFormat")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
        build(":ktlintFormat").apply {
            assertThat(task(":ktlintMainSourceSetFormat")!!.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
        }
    }

    @Test
    fun `Should apply ktlint version from extension`() {
        projectRoot.withCleanSources()

        projectRoot.buildFile().appendText("""

            ktlint.version = "0.26.0"
        """.trimIndent())

        build(":dependencies").apply {
            assertThat(output).contains("$KTLINT_CONFIGURATION_NAME - $KTLINT_CONFIGURATION_DESCRIPTION\n" +
                "\\--- com.github.shyiko:ktlint:0.26.0\n")
        }
    }

    @Test
    fun `Should apply pinterest ktlint version from extension when the requested version is 0_32_0`() {
        projectRoot.withCleanSources()

        projectRoot.buildFile().appendText(
            """

            ktlint.version = "0.32.0"
        """.trimIndent()
        )

        build(":dependencies").apply {
            assertThat(output).contains(
                "$KTLINT_CONFIGURATION_NAME - $KTLINT_CONFIGURATION_DESCRIPTION\n" +
                        "\\--- com.pinterest:ktlint:0.32.0\n"
            )
        }
    }

    @Test
    internal fun `Should check kotlin script file in project folder`() {
        projectRoot.withCleanSources()
        projectRoot.withCleanKotlinScript()

        build(":$KOTLIN_SCRIPT_CHECK_TASK").apply {
            assertThat(task(":$KOTLIN_SCRIPT_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Should fail check of kotlin script file in project folder`() {
        projectRoot.withCleanSources()
        projectRoot.withFailingKotlinScript()

        buildAndFail(":$KOTLIN_SCRIPT_CHECK_TASK").apply {
            assertThat(task(":$KOTLIN_SCRIPT_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.FAILED)
        }
    }

    @Test
    internal fun `Should not check kotlin script file in child project folder`() {
        projectRoot.withCleanSources()
        val additionalFolder = projectRoot.resolve("scripts/")
        additionalFolder.withFailingKotlinScript()

        build(":$KOTLIN_SCRIPT_CHECK_TASK").apply {
            assertThat(task(":$KOTLIN_SCRIPT_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
        }
    }

    @Test
    internal fun `Should check kts file in configured child project folder`() {
        projectRoot.withCleanSources()
        val additionalFolder = projectRoot.resolve("scripts/")
        additionalFolder.withCleanKotlinScript()
        projectRoot.buildFile().appendText("""

            ktlint.kotlinScriptAdditionalPaths { include fileTree("scripts/") }
        """.trimIndent())

        build(":$KOTLIN_SCRIPT_CHECK_TASK").apply {
            assertThat(task(":$KOTLIN_SCRIPT_CHECK_TASK")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Should apply internal git filter to check task`() {
        projectRoot.withCleanSources()
        projectRoot.withFailingSources()

        build(
            ":$CHECK_PARENT_TASK_NAME",
            "-P$FILTER_INCLUDE_PROPERTY_NAME=src/main/kotlin/clean-source.kt"
        ).run {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    internal fun `Git filter should respect already applied filters`() {
        projectRoot.withFailingSources()
        projectRoot.buildFile().appendText("""

            ktlint.filter { exclude("**/fail-source.kt") }
        """.trimIndent())

        build(
            ":$CHECK_PARENT_TASK_NAME",
            "-P$FILTER_INCLUDE_PROPERTY_NAME=src/main/kotlin/fail-source.kt"
        ).run {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
        }
    }

    @Test
    internal fun `Git filter should ignore task if no files related to it`() {
        projectRoot.withCleanSources()

        build(
            ":$CHECK_PARENT_TASK_NAME",
            "-P$FILTER_INCLUDE_PROPERTY_NAME=src/main/kotlin/failing-sources.kt"
        ).run {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
        }
    }

    @Test
    internal fun `Should enable experimental indentation rule`() {
        projectRoot.createSourceFile(
            "src/main/kotlin/C.kt",
            """
                class C {

                    private val Any.className
                        get() = this.javaClass.name
                            .fn()

                    private fun String.escape() =
                        this.fn()
                }
            """.trimIndent()
        )
        projectRoot.buildFile().appendText("""

            ktlint.enableExperimentalRules = true
        """.trimIndent())

        buildAndFail(":$CHECK_PARENT_TASK_NAME").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.FAILED)
        }
    }

    @Test
    internal fun `Should fail the build if ktlint version is less then 0_31_0 and experimental rules are enabled`() {
        projectRoot.withCleanSources()
        projectRoot.buildFile().appendText("""

            ktlint.version = "0.30.0"
            ktlint.enableExperimentalRules = true
        """.trimIndent())

        buildAndFail(":$CHECK_PARENT_TASK_NAME").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.FAILED)
            assertThat(output).contains("Experimental rules are supported since 0.31.0 ktlint version.")
        }
    }
}
