package org.jlleitschuh.gradle.ktlint

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test

class KtlintIdeaPluginTest : AbstractPluginTest() {
    @Before
    fun setupBuild() {
        projectRoot.apply {
            buildFile().writeText(
                """
                ${buildscriptBlockWithUnderTestPlugin()}

                ${pluginsBlockWithKotlinJvmPlugin()}

                apply plugin: "org.jlleitschuh.gradle.ktlint-idea"

                repositories {
                    gradlePluginPortal()
                }
            """.trimIndent()
            )
        }
    }

    @Test
    fun `applying helper plugin does not add the ktlint check task`() {
        build(":tasks").apply {
            assertThat(output.contains("ktlintCheck"), equalTo(false))
            // With space to not interfere with ktlintApplyToIdeaGlobally tasks
            assertThat(output.contains("ktlintApplyToIdea ", ignoreCase = true), equalTo(true))
        }
    }
}
