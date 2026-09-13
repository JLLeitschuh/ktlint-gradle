package org.jlleitschuh.gradle.ktlint.testdsl

import io.github.wakingrufus.testkit.project.TestProjectBuilder

fun TestProjectBuilder.defaultProject(
    kgpVersion: String = TestVersions.maxSupportedKotlinPluginVersion,
    kotlinPluginType: String = "jvm"
) {
    settings {
        pluginManagement {
            repositories {
                mavenLocal()
                gradlePluginPortal()
            }

            plugins {
                id("org.jetbrains.kotlin.$kotlinPluginType").version("kgpVersion")
                id("org.jlleitschuh.gradle.ktlint").version(TestVersions.pluginVersion)
            }
        }
    }
    rootProject {
        plugins {
            id("org.jetbrains.kotlin.jvm").version(TestVersions.maxSupportedKotlinPluginVersion)
            id("org.jlleitschuh.gradle.ktlint")
        }
        repositories {
            mavenCentral()
        }
    }
}
