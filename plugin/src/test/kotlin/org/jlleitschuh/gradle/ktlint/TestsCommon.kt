package org.jlleitschuh.gradle.ktlint

import org.intellij.lang.annotations.Language
import java.io.File

const val LOWEST_SUPPORTED_GRADLE_VERSION = "4.10"

fun File.buildFile() = resolve("build.gradle")

@Language("Groovy")
private fun pluginsBlockWithMainPluginAndKotlinPlugin(
    kotlinPluginId: String
) =
    """
        plugins {
            id '$kotlinPluginId'
            id 'org.jlleitschuh.gradle.ktlint'
        }
    """.trimIndent()

fun File.defaultProjectSetup() {
    kotlinPluginProjectSetup("org.jetbrains.kotlin.jvm")
}

fun File.kotlinPluginProjectSetup(
    kotlinPluginId: String
) {
    //language=Groovy
    buildFile().writeText(
        """
            ${pluginsBlockWithMainPluginAndKotlinPlugin(kotlinPluginId)}
            
            repositories {
                gradlePluginPortal()
            }

            import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

            ktlint.reporters = [ReporterType.CHECKSTYLE, ReporterType.PLAIN]
        """.trimIndent()
    )
}
