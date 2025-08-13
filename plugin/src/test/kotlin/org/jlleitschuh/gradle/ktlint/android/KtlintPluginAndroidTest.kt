package org.jlleitschuh.gradle.ktlint.android

import net.swiftzer.semver.SemVer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.hasOutcome
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.AbstractPluginTest
import org.jlleitschuh.gradle.ktlint.CHECK_PARENT_TASK_NAME
import org.jlleitschuh.gradle.ktlint.testdsl.TestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.androidProjectSetup
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.getMajorJavaVersion
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class KtlintPluginAndroidTest : AbstractPluginTest() {

    @ParameterizedTest
    @EnumSource(AndroidTestInput::class)
    fun `ktlint pass src java`(input: AndroidTestInput) {
        assumeFalse(input.minimumJava != null && getMajorJavaVersion() < input.minimumJava)
        assumeFalse(input.maximumJava != null && getMajorJavaVersion() > input.maximumJava)
        project(
            input.gradleVersion,
            projectSetup = androidProjectSetup(input.agpVersion, input.kotlinVersion, input.ktlintVersion)
        ) {
            withCleanSources("src/main/java/CleanSource.kt")
            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")).hasOutcome(TaskOutcome.SUCCESS)
                assertThat(task(":$kotlinScriptCheckTaskName")).hasOutcome(TaskOutcome.SUCCESS)
                assertThat(task(":$CHECK_PARENT_TASK_NAME")).hasOutcome(TaskOutcome.SUCCESS)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(AndroidTestInput::class)
    fun `ktlint pass src kotlin`(input: AndroidTestInput) {
        assumeFalse(input.minimumJava != null && getMajorJavaVersion() < input.minimumJava)
        assumeFalse(input.maximumJava != null && getMajorJavaVersion() > input.maximumJava)
        project(
            input.gradleVersion,
            projectSetup = androidProjectSetup(input.agpVersion, input.kotlinVersion, input.ktlintVersion)
        ) {
            withCleanSources("src/main/kotlin/CleanSource.kt")
            if (SemVer.parse(input.agpVersion) < SemVer(7)) {
                build(CHECK_PARENT_TASK_NAME) {
                    assertThat(output).contains("In AGP <7 kotlin source directories are not auto-detected.")
                    assertThat(task(":$mainSourceSetCheckTaskName")).hasOutcome(TaskOutcome.SKIPPED)
                    assertThat(task(":$kotlinScriptCheckTaskName")).hasOutcome(TaskOutcome.SUCCESS)
                    assertThat(task(":$CHECK_PARENT_TASK_NAME")).hasOutcome(TaskOutcome.SUCCESS)
                }
            } else {
                build(CHECK_PARENT_TASK_NAME) {
                    assertThat(task(":$mainSourceSetCheckTaskName")).hasOutcome(TaskOutcome.SUCCESS)
                    assertThat(task(":$kotlinScriptCheckTaskName")).hasOutcome(TaskOutcome.SUCCESS)
                    assertThat(task(":$CHECK_PARENT_TASK_NAME")).hasOutcome(TaskOutcome.SUCCESS)
                }
            }
        }
    }

    enum class AndroidTestInput(
        val gradleVersion: GradleVersion,
        val agpVersion: String,
        val kotlinVersion: String,
        val ktlintVersion: String? = null,
        val minimumJava: Int? = null,
        val maximumJava: Int? = null
    ) {
        MIN(
            GradleVersion.version(TestVersions.minSupportedGradleVersion),
            TestVersions.minAgpVersion,
            TestVersions.minSupportedKotlinPluginVersion,
            // old AGP doesn't properly set variant info which ktlint >= 1 requires
            "1.0.1",
            maximumJava = 17
        ),
        AGP_7_4(
            GradleVersion.version(TestVersions.minSupportedGradleVersion),
            "7.4.2",
            TestVersions.minSupportedKotlinPluginVersion,
            // old AGP doesn't properly set variant info which ktlint >= 1 requires
            "1.0.1",
            minimumJava = 11,
            maximumJava = 17
        ),
        MAX(
            GradleVersion.version(TestVersions.maxSupportedGradleVersion),
            TestVersions.maxAgpVersion,
            TestVersions.maxSupportedKotlinPluginVersion,
            minimumJava = 17
        )
    }
}
