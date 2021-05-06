package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jlleitschuh.gradle.ktlint.KtlintBasePlugin.Companion.LOWEST_SUPPORTED_GRADLE_VERSION
import org.jlleitschuh.gradle.ktlint.tasks.GenerateBaselineTask
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Runs [KotlinJsPluginTests] with the current version of Gradle.
 */
class GradleCurrentKtlintBaselineSupportTest : KtlintBaselineSupportTest()

/**
 * Runs [KotlinJsPluginTests] with lowest supported Gradle version.
 */
@Suppress("ClassName")
class GradleLowestSupportedKtlintBaselineSupportTest : KtlintBaselineSupportTest() {

    override fun gradleRunnerFor(
        vararg arguments: String,
        projectRoot: File
    ): GradleRunner =
        super.gradleRunnerFor(*arguments, projectRoot = projectRoot)
            .withGradleVersion(LOWEST_SUPPORTED_GRADLE_VERSION)
}

abstract class KtlintBaselineSupportTest : AbstractPluginTest() {
    @Test
    internal fun `Should generate empty baseline file on no style violations`() {
        with(projectRoot) {
            defaultProjectSetup()
            withCleanSources()
        }

        build(GenerateBaselineTask.NAME).apply {
            assertThat(task(":${GenerateBaselineTask.NAME}")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            val baselineFile = projectRoot.defaultBaselineFile
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

    @Test
    internal fun `Should generate baseline with found style violations`() {
        with(projectRoot) {
            defaultProjectSetup()
            withFailingSources()
            withFailingKotlinScript()
        }

        build(GenerateBaselineTask.NAME).apply {
            assertThat(task(":${GenerateBaselineTask.NAME}")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            val baselineFile = projectRoot.defaultBaselineFile
            assertThat(baselineFile).exists()
            assertThat(baselineFile.readText()).isEqualToNormalizingNewlines(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <baseline version="1.0">
                	<file name="kotlin-script-fail.kts">
                		<error line="1" column="15" source="no-trailing-spaces" />
                	</file>
                	<file name="src/main/kotlin/fail-source.kt">
                		<error line="1" column="5" source="no-multi-spaces" />
                		<error line="1" column="10" source="no-multi-spaces" />
                		<error line="1" column="15" source="no-multi-spaces" />
                	</file>
                </baseline>
                
                """.trimIndent()
            )
        }
    }

    @Test
    internal fun `Should overwrite existing baseline file`() {
        with(projectRoot) {
            defaultProjectSetup()
            withFailingSources()
        }

        build(GenerateBaselineTask.NAME)

        projectRoot.resolve(FAIL_SOURCE_FILE).delete()
        build(GenerateBaselineTask.NAME).apply {
            assertThat(task(":${GenerateBaselineTask.NAME}")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            val baselineFile = projectRoot.defaultBaselineFile
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

    @Test
    internal fun `Generate baseline task should work only when ktlint version higher then 0_41_0`() {
        with(projectRoot) {
            defaultProjectSetup()
            withFailingSources()

            buildFile().appendText(
                """
                
                ktlint {
                    version.set("0.40.0")
                }
                """.trimIndent()
            )

            build(GenerateBaselineTask.NAME).apply {
                assertThat(task(":${GenerateBaselineTask.NAME}")?.outcome).isEqualTo(TaskOutcome.SKIPPED)
                assertThat(output).containsSequence("Generate baseline only works starting from KtLint 0.41.0 version")
            }
        }
    }

    @Test
    internal fun `Should consider existing issues in baseline`() {
        with(projectRoot) {
            defaultProjectSetup()
            withFailingSources()

            build(GenerateBaselineTask.NAME)
            build(CHECK_PARENT_TASK_NAME).apply {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    @Test
    internal fun `Check task should still fail on file style violation that is not present in the baseline`() {
        with(projectRoot) {
            defaultProjectSetup()
            withFailingSources()

            build(GenerateBaselineTask.NAME)

            withFailingKotlinScript()
            buildAndFail(CHECK_PARENT_TASK_NAME).apply {
                assertThat(task(":$kotlinScriptCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            }
        }
    }

    @Test
    internal fun `Should fail the build if baseline file is present and ktlint version is less then 0_41_0`() {
        with(projectRoot) {
            defaultProjectSetup()
            withCleanSources()

            build(GenerateBaselineTask.NAME)

            buildFile().appendText(
                """
                
                ktlint {
                    version.set("0.40.0")
                }
                """.trimIndent()
            )

            buildAndFail(CHECK_PARENT_TASK_NAME).apply {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
                assertThat(output).contains("Baseline support is only enabled for KtLint versions 0.41.0+.")
            }
        }
    }

    private val File.defaultBaselineFile get() = resolve("config")
        .resolve("ktlint")
        .resolve("baseline.xml")
}
