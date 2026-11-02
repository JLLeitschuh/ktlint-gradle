package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.project

@GradleTestVersions
class EnforceSupportedKotlinVersionTest : AbstractPluginTest() {

    // a kotlin version that exists and is higher than the highest supported version of ktlint (2.2.21) to simulate a BOM forcing a newer version
    private val simulatedBomForcedKotlinVersion = "2.4.20"

    @CommonTest
    fun `should force the known Kotlin compiler version when enforceSupportedKotlinVersion is enabled`(
        gradleVersion: GradleVersion
    ) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.version = "1.8.0"
                ktlint.enforceSupportedKotlinVersion = true

                dependencies {
                    constraints {
                        add("ktlint", "org.jetbrains.kotlin:kotlin-stdlib:$simulatedBomForcedKotlinVersion")
                    }
                }

                ${printKtlintKotlinStdlibVersionTask()}
                """.trimIndent()
            )

            build("printKtlintKotlinStdlibVersion") {
                assertThat(output).contains("KOTLIN_STDLIB_VERSION=2.2.21")
            }
        }
    }

    @CommonTest
    fun `should not override a forced version when enforceSupportedKotlinVersion is disabled`(
        gradleVersion: GradleVersion
    ) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.version = "1.8.0"
                // enforceSupportedKotlinVersion left at its default (false)

                dependencies {
                    constraints {
                        add("ktlint", "org.jetbrains.kotlin:kotlin-stdlib:$simulatedBomForcedKotlinVersion")
                    }
                }

                ${printKtlintKotlinStdlibVersionTask()}
                """.trimIndent()
            )

            build("printKtlintKotlinStdlibVersion") {
                assertThat(output).contains("KOTLIN_STDLIB_VERSION=$simulatedBomForcedKotlinVersion")
            }
        }
    }

    private fun printKtlintKotlinStdlibVersionTask(): String =
        //language=Groovy
        """
        tasks.register("printKtlintKotlinStdlibVersion") {
            doLast {
                def stdlib = configurations.ktlint.incoming.resolutionResult.allComponents.find {
                    it.moduleVersion?.name == "kotlin-stdlib"
                }
                println("KOTLIN_STDLIB_VERSION=" + stdlib.moduleVersion.version)
            }
        }
        """.trimIndent()
}
