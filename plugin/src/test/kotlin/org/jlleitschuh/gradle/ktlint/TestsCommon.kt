package org.jlleitschuh.gradle.ktlint

import org.intellij.lang.annotations.Language
import java.io.File

fun File.buildFile() = resolve("build.gradle")

@Language("Groovy")
val pluginsBlockWithMainPluginAndKotlinJvm =
    """
        plugins {
            id 'org.jetbrains.kotlin.jvm'
            id 'org.jlleitschuh.gradle.ktlint'
        }
    """.trimIndent()

fun File.defaultProjectSetup() {
    //language=Groovy
    buildFile().writeText(
        """
            $pluginsBlockWithMainPluginAndKotlinJvm

            repositories {
                gradlePluginPortal()
            }

            import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

            ktlint.reporters = [ReporterType.CHECKSTYLE, ReporterType.PLAIN]
        """.trimIndent())
}