package org.jlleitschuh.gradle.ktlint

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo

import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class KtlintPluginTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Before
    fun setupBuild() {
        temporaryFolder.root.apply {
            resolve("settings.gradle").writeText("")
            resolve("build.gradle").writeText("""
                plugins {
                    id("org.jetbrains.kotlin.jvm") version "1.2.21"
                    id("org.jlleitschuh.gradle.ktlint")
                }

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
    fun build(vararg arguments: String) =
        gradleRunnerFor(*arguments).build()

    private
    fun buildAndFail(vararg arguments: String) =
        gradleRunnerFor(*arguments).buildAndFail()

    private
    fun gradleRunnerFor(vararg arguments: String) =
        GradleRunner.create()
            .withProjectDir(temporaryFolder.root)
            .withPluginClasspath()
            .withArguments(arguments.toList())

    private
    fun withCleanSources() =
        temporaryFolder.root.resolve("src/main/kotlin/source.kt").writeText("""val foo = "bar"""")

    private
    fun withFailingSources() =
        temporaryFolder.root.resolve("src/main/kotlin/source.kt").writeText("""val  foo    =     "bar"""")
}
