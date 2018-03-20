package org.jlleitschuh.gradle.ktlint

import org.gradle.testkit.runner.TaskOutcome

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo

import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class KtlintPluginTest : AbstractPluginTest() {

    @Before
    fun setupBuild() {
        projectRoot.apply {
            resolve("build.gradle").writeText("""
                ${buildscriptBlockWithUnderTestPlugin()}

                plugins {
                    id("org.jetbrains.kotlin.jvm") version "1.2.21"
                }

                apply plugin: "org.jlleitschuh.gradle.ktlint"

                repositories {
                    gradlePluginPortal()
                }
            """.trimIndent())
            resolve("src/main/kotlin").mkdirs()
        }
    }

    @Test
    fun `should fail check on failing sources`() {

        withFailingSources()

        buildAndFail("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.FAILED))
            assertThat(output, containsString("Unnecessary space(s)"))
        }
    }

    @Test
    fun `should succeed check on clean sources`() {

        withCleanSources()

        build("ktlintCheck").apply {
            assertThat(task(":ktlintMainCheck")!!.outcome, equalTo(TaskOutcome.SUCCESS))
        }
    }

    private
    fun withCleanSources() =
        projectRoot.resolve("src/main/kotlin/source.kt").writeText("""val foo = "bar"""")

    private
    fun withFailingSources() =
        projectRoot.resolve("src/main/kotlin/source.kt").writeText("""val  foo    =     "bar"""")
}
