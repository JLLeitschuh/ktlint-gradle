package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.project

@GradleTestVersions
class KtlintPluginsPropertiesTest : AbstractPluginTest() {

    @CommonTest
    fun shouldUseVersionFromKtlintPluginsPropertiesFileWhenPresent(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            // Create ktlint-plugins.properties file with version
            projectPath.resolve(KTLINT_PLUGINS_PROPERTIES_FILE_NAME).writeText(
                """
                ktlint-version=1.2.1
                """.trimIndent()
            )

            withCleanSources()

            buildGradle.appendText(
                """
                
                tasks.register("printKtlintVersion") {
                    doLast {
                        println("Ktlint version: " + ktlint.version.get())
                    }
                }
                """.trimIndent()
            )

            build("printKtlintVersion") {
                assertThat(output).contains("Ktlint version: 1.2.1")
            }
        }
    }

    @CommonTest
    fun shouldUseDefaultVersionWhenKtlintPluginsPropertiesFileIsAbsent(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()

            buildGradle.appendText(
                """
                
                tasks.register("printKtlintVersion") {
                    doLast {
                        println("Ktlint version: " + ktlint.version.get())
                    }
                }
                """.trimIndent()
            )

            build("printKtlintVersion") {
                assertThat(output).contains("Ktlint version: 1.5.0")
            }
        }
    }

    @CommonTest
    fun shouldAllowExplicitVersionOverrideEvenWhenPropertiesFileExists(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            // Create ktlint-plugins.properties file with version
            projectPath.resolve(KTLINT_PLUGINS_PROPERTIES_FILE_NAME).writeText(
                """
                ktlint-version=1.2.1
                """.trimIndent()
            )

            withCleanSources()

            buildGradle.appendText(
                """
                
                ktlint {
                    version = "1.3.0"
                }
                
                tasks.register("printKtlintVersion") {
                    doLast {
                        println("Ktlint version: " + ktlint.version.get())
                    }
                }
                """.trimIndent()
            )

            build("printKtlintVersion") {
                assertThat(output).contains("Ktlint version: 1.3.0")
            }
        }
    }

    @CommonTest
    fun shouldUseDefaultVersionWhenKtlintVersionPropertyIsBlank(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            // Create ktlint-plugins.properties file with blank version
            projectPath.resolve(KTLINT_PLUGINS_PROPERTIES_FILE_NAME).writeText(
                """
                ktlint-version=
                """.trimIndent()
            )

            withCleanSources()

            buildGradle.appendText(
                """
                
                tasks.register("printKtlintVersion") {
                    doLast {
                        println("Ktlint version: " + ktlint.version.get())
                    }
                }
                """.trimIndent()
            )

            build("printKtlintVersion") {
                assertThat(output).contains("Ktlint version: 1.5.0")
            }
        }
    }

    @CommonTest
    fun shouldUseDefaultVersionWhenPropertiesFileHasNoKtlintVersionProperty(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            // Create ktlint-plugins.properties file without ktlint-version
            projectPath.resolve(KTLINT_PLUGINS_PROPERTIES_FILE_NAME).writeText(
                """
                some-other-property=value
                """.trimIndent()
            )

            withCleanSources()

            buildGradle.appendText(
                """
                
                tasks.register("printKtlintVersion") {
                    doLast {
                        println("Ktlint version: " + ktlint.version.get())
                    }
                }
                """.trimIndent()
            )

            build("printKtlintVersion") {
                assertThat(output).contains("Ktlint version: 1.5.0")
            }
        }
    }
}
