package org.jlleitschuh.gradle.ktlint.testdsl

import net.swiftzer.semver.SemVer
import java.io.File

fun androidProjectSetup(
    agpVersion: String,
    kotlinPluginVersion: String,
    ktlintVersion: String? = null
): (File) -> Unit = {
    val ktLintOverride = ktlintVersion?.let { "ktlint { version = \"$it\" }\n" } ?: ""
    val setNamespace = if ((SemVer.parse(agpVersion) >= SemVer(7))) {
        "    namespace = \"com.example.myapp\"\n"
    } else {
        ""
    }
    //language=Groovy
    it.resolve("build.gradle").writeText(
        """
        |plugins {
        |    id("com.android.application")
        |    id("org.jetbrains.kotlin.android")
        |    id("org.jlleitschuh.gradle.ktlint")
        |}
        |
        |repositories {
        |    mavenCentral()
        |}
        |android {
        |    compileSdk = 33
        |$setNamespace}
        |$ktLintOverride
        """.trimMargin()
    )

    // before 4.2.0, AGP did not properly publish metadata for id resolution
    val oldAgpHack = if ((SemVer.parse(agpVersion) < SemVer(4, 2))) {
        """
        |    resolutionStrategy {
        |        eachPlugin {
        |            when (requested.id.id) {
        |                "com.android.application" -> useModule("com.android.tools.build:gradle:$agpVersion")
        |            }
        |        }
        |    }
        |
        """.trimMargin()
    } else {
        ""
    }
    val newAgp = if ((SemVer.parse(agpVersion) < SemVer(4, 2))) {
        ""
    } else {
        "    id(\"com.android.application\") version \"$agpVersion\"\n    "
    }

    //language=Groovy
    it.resolve("settings.gradle.kts").writeText(
        """
        |pluginManagement {
        |    repositories {
        |        mavenLocal()
        |        gradlePluginPortal()
        |        google()
        |        mavenCentral()
        |    }
        |
        |    plugins {
        |        id("org.jetbrains.kotlin.android") version ("$kotlinPluginVersion")
        |        id("org.jlleitschuh.gradle.ktlint") version ("${TestVersions.pluginVersion}")
        |    $newAgp}
        |$oldAgpHack}
        |
        """.trimMargin()
    )
}
