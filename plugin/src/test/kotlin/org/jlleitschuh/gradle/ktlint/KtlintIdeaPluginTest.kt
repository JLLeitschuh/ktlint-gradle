package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.testdsl.TestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class KtlintIdeaPluginTest : AbstractPluginTest() {

    @DisplayName("Applying helper plugin does not add KtLint check task")
    @Test
    fun applyDoesNotAddCheckTask() {
        project(
            GradleVersion.version(TestVersions.maxSupportedGradleVersion),
            projectSetup = {
                //language=Groovy
                it.resolve("settings.gradle").writeText(
                    """
                    pluginManagement {
                        repositories {
                            mavenLocal()
                            gradlePluginPortal()
                            google()
                        }

                        plugins {
                            id 'org.jetbrains.kotlin.jvm' version '${TestVersions.supportKotlinPluginVersion}'
                            id 'org.jlleitschuh.gradle.ktlint-idea' version '${TestVersions.pluginVersion}'
                        }
                    }
                    """.trimIndent()
                )

                //language=Groovy
                it.resolve("build.gradle").writeText(
                    """
                    plugins {
                        id 'org.jetbrains.kotlin.jvm'
                        id 'org.jlleitschuh.gradle.ktlint-idea'
                    }
                    """.trimIndent()
                )
            }
        ) {
            build(":tasks") {
                assertThat(output).doesNotContain(CHECK_PARENT_TASK_NAME)
                // With space to not interfere with ktlintApplyToIdeaGlobally tasks
                assertThat(output).containsIgnoringCase(APPLY_TO_IDEA_TASK_NAME)
            }
        }
    }
}
