package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Runs [KotlinJsPluginTests] with the current version of Gradle.
 */
class GradleCurrentKotlinJsPluginTests : KotlinJsPluginTests()

/**
 * Runs [KotlinJsPluginTests] with lowest supported Gradle version.
 */
@Suppress("ClassName")
class GradleLowestSupportedKotlinJsPluginTest : KotlinJsPluginTests() {

    override fun gradleRunnerFor(vararg arguments: String): GradleRunner =
        super.gradleRunnerFor(*arguments).withGradleVersion(LOWEST_SUPPORTED_GRADLE_VERSION)
}

/**
 * Contains all tests related to "org.jetbrains.kotlin.js" plugin support.
 */
abstract class KotlinJsPluginTests : AbstractPluginTest() {
    @BeforeEach
    internal fun setUp() {
        projectRoot.kotlinPluginProjectSetup("org.jetbrains.kotlin.js")
    }

    @Test
    internal fun `Should add check tasks`() {
        build("-m", "ktlintCheck").apply {
            val ktlintTasks = output
                .lineSequence()
                .toList()

            assertThat(ktlintTasks).anySatisfy {
                assertThat(it).contains("ktlintMainSourceSetCheck")
            }
            assertThat(ktlintTasks).anySatisfy {
                assertThat(it).contains("ktlintTestSourceSetCheck")
            }
        }
    }

    @Test
    internal fun `Should add format tasks`() {
        build("-m", "ktlintFormat").apply {
            val ktlintTasks = output
                .lineSequence()
                .toList()

            assertThat(ktlintTasks).anySatisfy {
                assertThat(it).contains("ktlintMainSourceSetFormat")
            }
            assertThat(ktlintTasks).anySatisfy {
                assertThat(it).contains("ktlintTestSourceSetFormat")
            }
        }
    }

    @Test
    internal fun `Should fail check task on un-formatted sources`() {
        projectRoot.withFailingSources()

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")?.outcome).isEqualTo(TaskOutcome.FAILED)
        }
    }
}
