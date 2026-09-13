package org.jlleitschuh.gradle.ktlint

import io.github.wakingrufus.testkit.assertions.TestKitAssertions.assertThat
import io.github.wakingrufus.testkit.project.testProject
import org.jlleitschuh.gradle.ktlint.testdsl.defaultProject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class ConfigurationsTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `test no reporters ktlint 1`() {
        val runner = testProject(projectDir) {
            defaultProject()
            rootProject {
                rawBuildScript("""ktlint.version = "1.0.0"""")
            }
        }
        val result = runner.run(
            listOf("dependencies", "--configuration", KTLINT_REPORTER_CONFIGURATION_NAME),
            withPluginClasspath = false
        ) {
            forwardOutput()
        }
        assertThat(result.output).doesNotContain("sarif4k")
    }

    @Test
    fun `test serif ktlint 1`() {
        val runner = testProject(projectDir) {
            defaultProject()
            rootProject {
                //language=kotlin
                rawBuildScript(
                    """
ktlint {
    version = "1.0.0"
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.SARIF)
    }
}
"""
                )
            }
        }
        val result = runner.run(
            listOf("dependencies", "--configuration", KTLINT_REPORTER_CONFIGURATION_NAME),
            withPluginClasspath = false
        ) {
            forwardOutput()
        }
        assertThat(result.output)
            .contains("com.pinterest.ktlint:ktlint-cli-reporter-sarif")
            .contains("sarif4k")
    }
}
