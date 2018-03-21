package org.jlleitschuh.gradle.ktlint

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

import org.gradle.util.TextUtil.normaliseFileSeparators

import org.junit.Rule
import org.junit.rules.TemporaryFolder

import java.io.File
import java.util.Properties

abstract class AbstractPluginTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    val projectRoot: File
        get() = temporaryFolder.root.resolve("plugin-test").apply { mkdirs() }

    protected
    fun buildscriptBlockWithUnderTestPlugin() =
        """
            buildscript {
                repositories { maven { url = "$testRepositoryPath" } }
                dependencies {
                    classpath("org.jlleitschuh.gradle:ktlint-gradle:${testProperties["version"]}")
                }
            }
        """.trimIndent()

    protected
    fun build(vararg arguments: String): BuildResult =
        gradleRunnerFor(*arguments).build()

    protected
    fun buildAndFail(vararg arguments: String): BuildResult =
        gradleRunnerFor(*arguments).buildAndFail()

    protected
    fun gradleRunnerFor(vararg arguments: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectRoot)
            .withArguments(arguments.toList())

    private
    val testRepositoryPath
        get() = normaliseFileSeparators(File("build/plugin-test-repository").absolutePath)

    protected
    val testProperties: Properties by lazy {
        javaClass.getResourceAsStream("/test.properties").use {
            Properties().apply { load(it) }
        }
    }
}
