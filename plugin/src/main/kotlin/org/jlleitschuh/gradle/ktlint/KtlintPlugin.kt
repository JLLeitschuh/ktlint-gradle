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
import org.gradle.api.plugins.HelpTasksPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.StopExecutionException
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KonanArtifactContainer
import org.jetbrains.kotlin.gradle.plugin.KonanExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileTask
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.io.File
import kotlin.reflect.KClass

const val VERIFICATION_GROUP = LifecycleBasePlugin.VERIFICATION_GROUP
const val FORMATTING_GROUP = "Formatting"
const val HELP_GROUP = HelpTasksPlugin.HELP_GROUP
const val CHECK_PARENT_TASK_NAME = "ktlintCheck"
const val FORMAT_PARENT_TASK_NAME = "ktlintFormat"
const val APPLY_TO_IDEA_TASK_NAME = "ktlintApplyToIdea"
const val APPLY_TO_IDEA_GLOBALLY_TASK_NAME = "ktlintApplyToIdeaGlobally"
val KOTLIN_EXTENSIONS = listOf("kt", "kts")

/**
 * Plugin that provides a wrapper over the `ktlint` project.
 */
open class KtlintPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.plugins.apply(KtlintHelperPlugin::class.java)

        // The extension has been added by the helper plugin above.
        val extension = target.extensions.getByName("ktlint") as KtlintExtension

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

    private fun addAdditionalRunArgs(extension: KtlintExtension, runArgs: MutableSet<String>) {
        if (extension.verbose) runArgs.add("--verbose")
        if (extension.debug) runArgs.add("--debug")
        if (extension.isAndroidFlagEnabled()) runArgs.add("--android")
        if (extension.ruleSets.isNotEmpty()) {
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
            android.set(target.provider { extension.isAndroidFlagEnabled() })
            ignoreFailures.set(target.provider { extension.ignoreFailures })
            outputToConsole.set(target.provider { extension.outputToConsole })
            ruleSets.set(target.provider { extension.ruleSets.toList() })
            reports.forEach { _, report ->
                report.enabled.set(target.provider {
                    val reporterType = report.reporterType
                    reporterAvailable(extension.version, reporterType) && extension.reporters.contains(reporterType)
                })
                report.outputFile.set(target.layout.buildDirectory.file(target.provider {
                    "reports/ktlint/ktlint-$sourceSetName.${report.reporterType.fileExtension}"
                }))
            }
        }
    }

    private fun reporterAvailable(version: String, reporter: ReporterType) =
        SemVer.parse(version) >= reporter.availableSinceVersion

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

    private inline fun <reified T> Convention.getPluginHelper() = getPlugin(T::class.java)
}

/**
 * Helper plugin that only applies tasks that don't modify the "check" task.
 */
open class KtlintHelperPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("ktlint", KtlintExtension::class.java)

        if (target == target.rootProject) {
            /*
             * Only add these tasks if we are applying to the root project.
             */
            addApplyToIdeaTasks(target, extension)
        }
    }

    private fun addApplyToIdeaTasks(rootProject: Project, extension: KtlintExtension) {
        rootProject.afterEvaluate {
            if (rootProject.tasks.findByName(APPLY_TO_IDEA_TASK_NAME) == null) {
                val ktLintConfig = createConfiguration(rootProject, extension)

                if (extension.isApplyToIdeaPerProjectAvailable()) {
                    rootProject.taskHelper<KtlintApplyToIdeaTask>(APPLY_TO_IDEA_TASK_NAME) {
                        group = HELP_GROUP
                        description = "Generates IDEA built-in formatter rules and apply them to the project." +
                            "It will overwrite existing ones."
                        classpath.setFrom(ktLintConfig)
                        android.set(rootProject.provider { extension.isAndroidFlagEnabled() })
                        globally.set(rootProject.provider { false })
                    }
                }

                rootProject.taskHelper<KtlintApplyToIdeaTask>(APPLY_TO_IDEA_GLOBALLY_TASK_NAME) {
                    group = HELP_GROUP
                    description = "Generates IDEA built-in formatter rules and apply them globally " +
                        "(in IDEA user settings folder). It will overwrite existing ones."
                    classpath.setFrom(ktLintConfig)
                    android.set(rootProject.provider { extension.isAndroidFlagEnabled() })
                    globally.set(rootProject.provider { true })
                }
            }
        }
    }

    /**
     * Checks if apply code style to IDEA IDE per project is availalbe.
     *
     * Available since KtLint version `0.22.0`.
     */
    private fun KtlintExtension.isApplyToIdeaPerProjectAvailable() = SemVer.parse(version) >= SemVer(0, 22, 0)
}

private fun createConfiguration(target: Project, extension: KtlintExtension) =
    target.configurations.maybeCreate("ktlint").apply {
        target.dependencies.add(
            this.name,
            mapOf(
                "group" to "com.github.shyiko",
                "name" to "ktlint",
                "version" to extension.version
            )
        )
    }

private inline fun <reified T : Task> Project.taskHelper(name: String, noinline configuration: T.() -> Unit): T {
    return this.tasks.create(name, T::class.java, configuration)
}

/**
 * Android option is available from ktlint 0.12.0.
 */
private fun KtlintExtension.isAndroidFlagEnabled() =
    android && SemVer.parse(version) >= SemVer(0, 12, 0)
