package org.jlleitschuh.gradle.ktlint

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.FeaturePlugin
import com.android.build.gradle.InstantAppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestPlugin
import net.swiftzer.semver.SemVer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.StopExecutionException
import org.jetbrains.kotlin.gradle.plugin.KonanArtifactContainer
import org.jetbrains.kotlin.gradle.plugin.KonanExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileTask
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.io.File
import kotlin.reflect.KClass

const val VERIFICATION_GROUP = "Verification"
const val FORMATTING_GROUP = "Formatting"
const val CHECK_PARENT_TASK_NAME = "ktlintCheck"
const val FORMAT_PARENT_TASK_NAME = "ktlintFormat"
val KOTLIN_EXTENSIONS = listOf("kt", "kts")

/**
 * Task that provides a wrapper over the `ktlint` project.
 */
open class KtlintPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val extension = target.extensions.create("ktlint", KtlintExtension::class.java)

        addKtLintTasksToKotlinPlugin(target, extension)

        // Checking subprojects as well
        target.subprojects {
            addKtLintTasksToKotlinPlugin(it, extension)
        }
    }

    private fun addKtLintTasksToKotlinPlugin(target: Project, extension: KtlintExtension) {
        target.pluginManager.withPlugin("kotlin", applyKtLint(target, extension))
        target.pluginManager.withPlugin("kotlin2js", applyKtLint(target, extension))
        target.pluginManager.withPlugin("kotlin-platform-common", applyKtLint(target, extension))
        target.pluginManager.withPlugin("kotlin-android", applyKtLintToAndroid(target, extension))
        target.pluginManager.withPlugin("konan", applyKtLintNative(target, extension))
    }

    private fun applyKtLint(
        target: Project,
        extension: KtlintExtension
    ): (AppliedPlugin) -> Unit {
        return {
            target.afterEvaluate {
                val ktLintConfig = createConfiguration(target, extension)

                target.theHelper<JavaPluginConvention>().sourceSets.forEach {
                    val kotlinSourceSet: SourceDirectorySet = (it as HasConvention)
                            .convention
                            .getPluginHelper<KotlinSourceSet>()
                            .kotlin
                    val checkTask = createCheckTask(target, extension, it.name, ktLintConfig, kotlinSourceSet.sourceDirectories)
                    addKtlintCheckTaskToProjectMetaCheckTask(target, checkTask)
                    setCheckTaskDependsOnKtlintCheckTask(target, checkTask)

                    val runArgs = kotlinSourceSet.sourceDirectories.files.flatMap { baseDir ->
                        KOTLIN_EXTENSIONS.map { "${baseDir.path}/**/*.$it" }
                    }.toMutableSet()
                    addAdditionalRunArgs(extension, runArgs)

                    val ktlintSourceSetFormatTask = createFormatTask(target, it.name, ktLintConfig, kotlinSourceSet, runArgs)
                    addKtlintFormatTaskToProjectMetaFormatTask(target, ktlintSourceSetFormatTask)
                }
            }
        }
    }

    private fun applyKtLintToAndroid(
        target: Project,
        extension: KtlintExtension
    ): (AppliedPlugin) -> Unit {
        return {
            target.afterEvaluate {
                val ktLintConfig = createConfiguration(target, extension)

                val variantManager = when {
                    target.plugins.hasPlugin(AppPlugin::class.java) -> target.plugins.findPlugin(AppPlugin::class.java)?.variantManager
                    target.plugins.hasPlugin(LibraryPlugin::class.java) -> target.plugins.findPlugin(LibraryPlugin::class.java)?.variantManager
                    target.plugins.hasPlugin(InstantAppPlugin::class.java) -> target.plugins.findPlugin(InstantAppPlugin::class.java)?.variantManager
                    target.plugins.hasPlugin(FeaturePlugin::class.java) -> target.plugins.findPlugin(FeaturePlugin::class.java)?.variantManager
                    target.plugins.hasPlugin(TestPlugin::class.java) -> target.plugins.findPlugin(TestPlugin::class.java)?.variantManager
                    else -> throw StopExecutionException("Must be applied with 'android' or 'android-library' plugin.")
                }

                variantManager?.variantScopes?.forEach {
                    val sourceDirs = it.variantData.javaSources
                            .fold(mutableListOf<File>()) { acc, configurableFileTree ->
                                acc.add(configurableFileTree.dir)
                                acc
                            }
                    // Don't use it.variantData.javaSources directly as it will trigger some android tasks execution
                    val kotlinSourceDir = target.files(*sourceDirs.toTypedArray())
                    val runArgs = it.variantData.javaSources.map { "${it.dir.path}/**/*.kt" }.toMutableSet()
                    addAdditionalRunArgs(extension, runArgs)

                    val checkTask = createCheckTask(target, extension, it.fullVariantName, ktLintConfig, sourceDirs)
                    addKtlintCheckTaskToProjectMetaCheckTask(target, checkTask)
                    setCheckTaskDependsOnKtlintCheckTask(target, checkTask)

                    val ktlintSourceSetFormatTask = createFormatTask(target, it.fullVariantName, ktLintConfig, kotlinSourceDir, runArgs)
                    addKtlintFormatTaskToProjectMetaFormatTask(target, ktlintSourceSetFormatTask)
                }
            }
        }
    }

    private fun applyKtLintNative(
        project: Project,
        extension: KtlintExtension
    ): (AppliedPlugin) -> Unit {
        return {
            project.afterEvaluate {
                val ktLintConfig = createConfiguration(project, extension)

                val compileTargets = project.theHelper<KonanExtension>().targets

                project.theHelper<KonanArtifactContainer>().forEach { konanBuildingConfig ->
                    val sourceDirectoriesList = mutableListOf<FileCollection>()
                    compileTargets.forEach { target ->
                        val compileTask = konanBuildingConfig.findByTarget(target)
                        if (compileTask != null) {
                            val sourceFiles = (compileTask as KonanCompileTask).srcFiles
                            sourceDirectoriesList.addAll(sourceFiles)
                        }
                    }
                    if (sourceDirectoriesList.isNotEmpty()) {
                        val checkTask = createCheckTask(project, extension, it.name, ktLintConfig,
                                sourceDirectoriesList)
                        addKtlintCheckTaskToProjectMetaCheckTask(project, checkTask)
                        setCheckTaskDependsOnKtlintCheckTask(project, checkTask)

                        val kotlinSourceSet = sourceDirectoriesList.reduce { acc, fileCollection ->
                            acc.add(fileCollection)
                        }
                        val runArgs = kotlinSourceSet.files.map { "${it.path}/**/*.kt" }.toMutableSet()
                        addAdditionalRunArgs(extension, runArgs)

                        val ktlintSourceSetFormatTask = createFormatTask(project, it.name, ktLintConfig,
                                kotlinSourceSet, runArgs)
                        addKtlintFormatTaskToProjectMetaFormatTask(project, ktlintSourceSetFormatTask)
                    }
                }
            }
        }
    }

    private fun createConfiguration(target: Project, extension: KtlintExtension) =
            target.configurations.maybeCreate("ktlint").apply {
                target.dependencies.add(this.name,
                        mapOf("group" to "com.github.shyiko", "name" to "ktlint", "version" to extension.version))
            }

    private fun addAdditionalRunArgs(extension: KtlintExtension, runArgs: MutableSet<String>) {
        if (extension.verbose) runArgs.add("--verbose")
        if (extension.debug) runArgs.add("--debug")
        if (extension.android && SemVer.parse(extension.version) >= SemVer(0, 12, 0)) {
            // Android option is available from ktlint 0.12.0
            runArgs.add("--android")
        }
        if (extension.ruleSets.size > 0) {
            extension.ruleSets.forEach { runArgs.add("--ruleset=$it") }
        }
    }

    private fun addKtlintCheckTaskToProjectMetaCheckTask(target: Project, checkTask: Task) {
        target.getMetaKtlintCheckTask().dependsOn(checkTask)
        if (target.rootProject != target) {
            target.rootProject.getMetaKtlintCheckTask().dependsOn(checkTask)
        }
    }

    private fun addKtlintFormatTaskToProjectMetaFormatTask(target: Project, formatTask: Task) {
        target.getMetaKtlintFormatTask().dependsOn(formatTask)
        if (target.rootProject != target) {
            target.rootProject.getMetaKtlintFormatTask().dependsOn(formatTask)
        }
    }

    private fun createFormatTask(
        target: Project,
        sourceSetName: String,
        ktLintConfig: Configuration,
        kotlinSourceSet: FileCollection,
        runArgs: MutableSet<String>
    ): Task {
        return target.taskHelper<JavaExec>("ktlint${sourceSetName.capitalize()}Format") {
            group = FORMATTING_GROUP
            description = "Runs a check against all .kt files to ensure that they are formatted according to ktlint."
            main = "com.github.shyiko.ktlint.Main"
            classpath = ktLintConfig
            inputs.files(kotlinSourceSet)
            // This copies the list
            val sourcePathsWithFormatFlag = runArgs.toMutableList()
            // Prepend the format flag to the beginning of the list
            sourcePathsWithFormatFlag.add(0, "-F")
            args(sourcePathsWithFormatFlag)
        }
    }

    private fun createCheckTask(
        target: Project,
        extension: KtlintExtension,
        sourceSetName: String,
        ktLintConfig: Configuration,
        kotlinSourceDirectories: Iterable<*>
    ): Task {
        return target.taskHelper<KtlintCheck>("ktlint${sourceSetName.capitalize()}Check") {
            group = VERIFICATION_GROUP
            description = "Runs a check against all .kt files to ensure that they are formatted according to ktlint."
            classpath.setFrom(ktLintConfig)
            sourceDirectories.setFrom(kotlinSourceDirectories)
            verbose.set(target.provider { extension.verbose })
            debug.set(target.provider { extension.debug })
            android.set(target.provider { extension.android && SemVer.parse(extension.version) >= SemVer(0, 12, 0) })
            ignoreFailures.set(target.provider { extension.ignoreFailures })
            outputToConsole.set(target.provider { extension.outputToConsole })
            ruleSets.set(target.provider { extension.ruleSets.toList() })
            reports.forEach { _, report ->
                report.enabled.set(target.provider {
                    val reporterType = report.reporterType
                    reporterAvailable(extension.version, reporterType) && (extension.reporters.contains(reporterType) || extension.reporter == reporterType)
                })
                report.outputFile.set(target.layout.buildDirectory.file(target.provider {
                    "reports/ktlint/ktlint-$sourceSetName.${report.reporterType.fileExtension}"
                }))
            }
        }
    }

    private fun reporterAvailable(version: String, reporter: ReporterType) = SemVer.parse(version) >= reporter.availableSinceVersion

    private fun Project.getMetaKtlintCheckTask(): Task = this.tasks.findByName(CHECK_PARENT_TASK_NAME)
            ?: this.task(CHECK_PARENT_TASK_NAME).apply {
                group = VERIFICATION_GROUP
                description = "Runs ktlint on all kotlin sources in this project."
            }

    private fun Project.getMetaKtlintFormatTask(): Task = this.tasks.findByName(FORMAT_PARENT_TASK_NAME)
            ?: this.task(FORMAT_PARENT_TASK_NAME).apply {
                group = FORMATTING_GROUP
                description = "Runs the ktlint formatter on all kotlin sources in this project."
            }

    private fun setCheckTaskDependsOnKtlintCheckTask(project: Project, ktlintCheck: Task) {
        project.tasks.findByName("check")?.dependsOn(ktlintCheck)
    }

    /*
     * Helper functions used until Gradle Script Kotlin solidifies it's plugin API.
     */

    private inline fun <reified T : Any> Project.theHelper() =
        theHelper(T::class)

    private fun <T : Any> Project.theHelper(extensionType: KClass<T>) =
        convention.findPlugin(extensionType.java) ?: convention.getByType(extensionType.java)

    private inline fun <reified T : Task> Project.taskHelper(name: String, noinline configuration: T.() -> Unit): T {
        return this.tasks.create(name, T::class.java, configuration)
    }

    private inline fun <reified T> Convention.getPluginHelper() = getPlugin(T::class.java)
}
