package org.jlleitschuh.gradle.ktlint

import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import java.io.File

class KtlintPluginVersionTest : AbstractPluginTest() {

    private fun File.buildScriptUsingKtlintVersion(version: String) {
        buildFile().writeText("""
                ${buildscriptBlockWithUnderTestPlugin()}

                ${pluginsBlockWithKotlinJvmPlugin()}

                apply plugin: "org.jlleitschuh.gradle.ktlint"

                repositories {
                    gradlePluginPortal()
                }

                buildDir = file("directory with spaces")

                ktlint {
                    version = "$version"
                }
            """.trimIndent())
    }

    @Before
    fun setup() {
        projectRoot.withCleanSources()
    }

    @Test
    fun `with ktlint version equal to 0_22`() {
        projectRoot.buildScriptUsingKtlintVersion("0.22.0")
        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome, equalTo(TaskOutcome.SUCCESS))
        }
    }

    @Test
    fun `with ktlint version less than 0_22`() {
        projectRoot.buildScriptUsingKtlintVersion("0.21.0")
        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainSourceSetCheck")!!.outcome, equalTo(TaskOutcome.FAILED))
        }
    }
}
