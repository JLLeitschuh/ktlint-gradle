package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import kotlin.reflect.KClass

/**
 * Task that provides a wrapper over the `ktlint` project.
 */
open class KtlintPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val formattingGroup = "formatting"

        val extension = target.extensions.create("ktlint", KtlintExtension::class.java)

        val ktlintTask = target.task("ktlintCheck").apply {
            group = formattingGroup
            description = "Runs ktlint on all kotlin sources in this project."
        }
        val ktlintFormatTask = target.task("ktlintFormat").apply {
            group = formattingGroup
            description = "Runs the ktlint formatter on all kotlin sources in this project."
        }

        // Only apply this plugin to projects that have the kotlin plugin applied.
        target.pluginManager.withPlugin("kotlin") {
            val ktLintConfig = target.configurations.maybeCreate("ktlint")

            target.dependencies.add(ktLintConfig.name,
                mapOf("group" to "com.github.shyiko", "name" to "ktlint", "version" to extension.version))
            target.afterEvaluate {
                val sourceSets = target.theHelper<JavaPluginConvention>().sourceSets
                sourceSets.forEach {
                    val kotlinSourceSet: SourceDirectorySet = (it as HasConvention)
                        .convention
                        .getPluginHelper<KotlinSourceSet>()
                        .kotlin
                    val runArgs = kotlinSourceSet.sourceDirectories.files.map { "${it.path}/**/*.kt" }.toMutableList()

                    // Add the args to enable verbose and debug mode.
                    if (extension.verbose) runArgs.add("--verbose")
                    if (extension.debug) runArgs.add("--debug")

                    val ktlintSourceSetTask = target.taskHelper<JavaExec>("ktlint${it.name.capitalize()}Check") {
                        group = formattingGroup
                        description = "Runs a check against all .kt files to ensure that they are formatted according to ktlint."
                        main = "com.github.shyiko.ktlint.Main"
                        classpath = ktLintConfig
                        inputs.dir(kotlinSourceSet)
                        args(runArgs)
                    }
                    ktlintTask.dependsOn(ktlintSourceSetTask)

                    val ktlintSourceSetFormatTask = target.taskHelper<JavaExec>("ktlint${it.name.capitalize()}Format") {
                        group = formattingGroup
                        description = "Runs a check against all .kt files to ensure that they are formatted according to ktlint."
                        main = "com.github.shyiko.ktlint.Main"
                        classpath = ktLintConfig
                        inputs.dir(kotlinSourceSet)
                        // This copies the list
                        val sourcePathsWithFormatFlag = runArgs.toMutableList()
                        // Prepend the format flag to the beginning of the list
                        sourcePathsWithFormatFlag.add(0, "-F")
                        args(sourcePathsWithFormatFlag)
                    }
                    ktlintFormatTask.dependsOn(ktlintSourceSetFormatTask)
                }
            }
        }
    }

    /*
     * Helper functions used until Gradle Script Kotlin solidifies it's plugin API.
     *
     */

    private inline fun <reified T : Any> Project.theHelper() =
        theHelper(T::class)

    private fun <T : Any> Project.theHelper(extensionType: KClass<T>) =
        convention.findPlugin(extensionType.java) ?: convention.getByType(extensionType.java)!!


    private inline fun <reified T : Task> Project.taskHelper(name: String): T {
        return this.tasks.create(name, T::class.java)!!
    }

    private inline fun <reified T : Task> Project.taskHelper(name: String, noinline configuration: T.() -> Unit): T {
        return this.tasks.create(name, T::class.java, configuration)!!
    }

    private inline fun <reified T> Convention.getPluginHelper() = getPlugin(T::class.java)
}