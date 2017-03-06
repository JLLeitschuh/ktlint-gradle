package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec
import org.gradle.script.lang.kotlin.create
import org.gradle.script.lang.kotlin.dependencies
import org.gradle.script.lang.kotlin.getPlugin
import org.gradle.script.lang.kotlin.task
import org.gradle.script.lang.kotlin.the
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet


open class KtLintPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val formattingGroup = "formatting"

        target.pluginManager.withPlugin("kotlin") {
            val ktLintConfig = target.configurations.maybeCreate("ktlint")

            target.dependencies {
                add(ktLintConfig.name,
                    create(group = "com.github.shyiko", name = "ktlint", version = "0.4.0"))
            }
            target.afterEvaluate {
                val sourceSets = target.the<JavaPluginConvention>().sourceSets
                sourceSets.forEach {
                    val kotlinSourceSet: SourceDirectorySet = (it as HasConvention)
                        .convention
                        .getPlugin<KotlinSourceSet>()
                        .kotlin
                    val sourcePaths = kotlinSourceSet.sourceDirectories.files.map { "${it.path}/**/*.kt" }.toMutableList()
                    // TODO: Add ability to enable debugging and verbose mode
                    target.task<JavaExec>("ktlint${it.name.capitalize()}") {
                        group = formattingGroup
                        description = "Runs a check against all .kt files to ensure that they are formatted according to ktlint."
                        main = "com.github.shyiko.ktlint.Main"
                        classpath = ktLintConfig
                        inputs.dir(kotlinSourceSet)
                        args(sourcePaths)
                    }

                    target.task<JavaExec>("ktlint${it.name.capitalize()}Format") {
                        group = formattingGroup
                        description = "Runs a check against all .kt files to ensure that they are formatted according to ktlint."
                        main = "com.github.shyiko.ktlint.Main"
                        classpath = ktLintConfig
                        inputs.dir(kotlinSourceSet)
                        // This copies the list
                        val sourcePathsWithFormatFlag = sourcePaths.toMutableList()
                        sourcePathsWithFormatFlag.add(0, "-F")
                        args(sourcePathsWithFormatFlag)
                    }
                }
            }
        }
    }
}