package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KtlintIdeaPluginTest : AbstractPluginTest() {
    @BeforeEach
    fun setupBuild() {
        projectRoot.apply {
            buildFile().writeText(
                """
                ${pluginsBlockWithIdeaPlugin()}

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
            assertThat(output).doesNotContain("ktlintCheck")
            // With space to not interfere with ktlintApplyToIdeaGlobally tasks
            assertThat(output).containsIgnoringCase("ktlintApplyToIdea")
        }
    }
}
