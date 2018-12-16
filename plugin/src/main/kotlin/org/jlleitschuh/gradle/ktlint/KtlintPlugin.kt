package org.jlleitschuh.gradle.ktlint

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeComponent
import org.jetbrains.kotlin.gradle.plugin.konan.KonanArtifactContainer
import org.jetbrains.kotlin.gradle.plugin.konan.KonanExtension
import shadow.org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileTask
import java.util.concurrent.Callable
import kotlin.reflect.KClass

/**
 * Plugin that provides a wrapper over the `ktlint` project.
 */
open class KtlintPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val extension = target.plugins.apply(KtlintBasePlugin::class.java).extension
        // Apply the idea plugin
        target.plugins.apply(KtlintIdeaPlugin::class.java)

        addKtLintTasksToKotlinPlugin(target, extension)
    }

    private fun addKtLintTasksToKotlinPlugin(target: Project, extension: KtlintExtension) {
        target.plugins.withId("kotlin", applyKtLint(target, extension))
        target.plugins.withId("kotlin2js", applyKtLint(target, extension))
        target.plugins.withId("kotlin-platform-common", applyKtLint(target, extension))
        target.plugins.withId("kotlin-android", applyKtLintToAndroid(target, extension))
        target.plugins.withId("konan", applyKtLintKonanNative(target, extension))
        target.plugins.withId(
            "org.jetbrains.kotlin.native",
            applyKtLintNative(target, extension)
        )
        target.plugins.withId(
            "org.jetbrains.kotlin.multiplatform",
            applyKtlintMultiplatform(target, extension)
        )
    }

    private fun applyKtlintMultiplatform(
        target: Project,
        extension: KtlintExtension
    ): (Plugin<in Any>) -> Unit = {
        val ktLintConfig = createConfiguration(target, extension)
        val multiplatformExtension = target.extensions.getByType(KotlinMultiplatformExtension::class.java)

        multiplatformExtension.sourceSets.all { sourceSet ->
            val checkTask = createCheckTask(
                target,
                extension,
                sourceSet.name,
                ktLintConfig,
                sourceSet.kotlin.sourceDirectories
            )

            addKtlintCheckTaskToProjectMetaCheckTask(target, checkTask)
            setCheckTaskDependsOnKtlintCheckTask(target, checkTask)

            val ktlintSourceSetFormatTask = createFormatTask(
                target,
                extension,
                sourceSet.name,
                ktLintConfig,
                sourceSet.kotlin.sourceDirectories
            )

            addKtlintFormatTaskToProjectMetaFormatTask(target, ktlintSourceSetFormatTask)
        }

        multiplatformExtension.targets.all { kotlinTarget ->
            when (kotlinTarget.platformType) {
                KotlinPlatformType.androidJvm -> {
                    val androidConfigureAction: (Plugin<Any>) -> Unit = {
                        target.extensions.configure(BaseExtension::class.java) { ext ->
                            ext.addVariantsMetaTasks(target, kotlinTarget.targetName)
                        }
                    }
                    target.plugins.withId("com.android.application", androidConfigureAction)
                    target.plugins.withId("com.android.library", androidConfigureAction)
                    target.plugins.withId("com.android.instantapp", androidConfigureAction)
                    target.plugins.withId("com.android.feature", androidConfigureAction)
                    target.plugins.withId("com.android.test", androidConfigureAction)
                }
                else -> Unit
            }
        }
    }

    private fun applyKtLint(
        target: Project,
        extension: KtlintExtension
    ): (Plugin<in Any>) -> Unit {
        return {
            val ktLintConfig = createConfiguration(target, extension)

            target.theHelper<JavaPluginConvention>().sourceSets.forEach { sourceSet ->
                val kotlinSourceSet: SourceDirectorySet = (sourceSet as HasConvention)
                    .convention
                    .getPluginHelper<KotlinSourceSet>()
                    .kotlin
                val checkTask = createCheckTask(
                    target,
                    extension,
                    sourceSet.name,
                    ktLintConfig,
                    kotlinSourceSet.sourceDirectories
                )

                addKtlintCheckTaskToProjectMetaCheckTask(target, checkTask)
                setCheckTaskDependsOnKtlintCheckTask(target, checkTask)

                val ktlintSourceSetFormatTask = createFormatTask(
                    target,
                    extension,
                    sourceSet.name,
                    ktLintConfig,
                    kotlinSourceSet.sourceDirectories
                )

                addKtlintFormatTaskToProjectMetaFormatTask(target, ktlintSourceSetFormatTask)
            }
        }
    }

    private fun applyKtLintToAndroid(
        target: Project,
        extension: KtlintExtension
    ): (Plugin<in Any>) -> Unit {
        return {
            val ktLintConfig = createConfiguration(target, extension)

            fun createTasks(
                sourceSetName: String,
                sources: FileCollection
            ) {
                val checkTask = createCheckTask(
                    target,
                    extension,
                    sourceSetName,
                    ktLintConfig,
                    sources
                )

                addKtlintCheckTaskToProjectMetaCheckTask(target, checkTask)
                setCheckTaskDependsOnKtlintCheckTask(target, checkTask)

                val ktlintSourceSetFormatTask = createFormatTask(
                    target,
                    extension,
                    sourceSetName,
                    ktLintConfig,
                    sources
                )

                addKtlintFormatTaskToProjectMetaFormatTask(target, ktlintSourceSetFormatTask)
            }

            /*
             * Variant manager returns all sources for variant,
             * so most probably main source set maybe checked several times.
             * This approach creates one check tasks per one source set.
             */
            val pluginConfigureAction: (Plugin<Any>) -> Unit = {
                target.extensions.configure(BaseExtension::class.java) { ext ->
                    ext.sourceSets { sourceSet ->
                        sourceSet.all { androidSourceSet ->
                            // Passing Callable, so returned FileCollection, will lazy evaluate it
                            // only when task will need it.
                            // Solves the problem of having additional source dirs in
                            // current AndroidSourceSet, that are not available on eager
                            // evaluation.
                            createTasks(
                                androidSourceSet.name,
                                target.files(Callable { androidSourceSet.java.srcDirs })
                            )
                        }
                    }

                    ext.addVariantsMetaTasks(target)
                }
            }

            target.plugins.withId("com.android.application", pluginConfigureAction)
            target.plugins.withId("com.android.library", pluginConfigureAction)
            target.plugins.withId("com.android.instantapp", pluginConfigureAction)
            target.plugins.withId("com.android.feature", pluginConfigureAction)
            target.plugins.withId("com.android.test", pluginConfigureAction)
        }
    }

    private fun BaseExtension.addVariantsMetaTasks(
        target: Project,
        multiplatformTargetName: String? = null
    ) {
        variants?.all { variant ->
            val variantCheckTask = target.getAndroidVariantMetaKtlintCheckTask(variant.name, multiplatformTargetName)
            val variantFormatTask = target.getAndroidVariantMetaKtlintFormatTask(variant.name, multiplatformTargetName)
            variant.sourceSets.forEach { sourceSet ->
                val sourceSetName = "${multiplatformTargetName?.capitalize() ?: ""}${sourceSet.name.capitalize()}"
                variantCheckTask.configure { it.dependsOn(sourceSetName.sourceSetCheckTaskName()) }
                variantFormatTask.configure { it.dependsOn(sourceSetName.sourceSetFormatTaskName()) }
            }
        }
    }

    private fun applyKtLintKonanNative(
        project: Project,
        extension: KtlintExtension
    ): (Plugin<in Any>) -> Unit {
        return {
            val ktLintConfig = createConfiguration(project, extension)

            val compileTargets = project.theHelper<KonanExtension>().targets
            project.theHelper<KonanArtifactContainer>().whenObjectAdded { buildConfig ->
                addTasksForNativePlugin(project, extension, buildConfig.name, ktLintConfig) {
                    compileTargets.fold(initial = emptyList()) { acc, target ->
                        val compileTask = buildConfig.findByTarget(target)
                        if (compileTask != null) {
                            val sourceFiles = (compileTask as KonanCompileTask).srcFiles
                            acc + sourceFiles
                        } else {
                            acc
                        }
                    }
                }
            }
        }
    }

    private fun applyKtLintNative(
        project: Project,
        extension: KtlintExtension
    ): (Plugin<in Any>) -> Unit {
        return {
            val ktLintConfig = createConfiguration(project, extension)

            project.components.withType(KotlinNativeComponent::class.java) { component ->
                addTasksForNativePlugin(project, extension, component.name, ktLintConfig) {
                    component.konanTargets.get()
                        .fold(initial = emptyList()) { acc, nativeTarget ->
                            acc + listOf(component.sources.getAllSources(nativeTarget))
                        }
                }
            }
        }
    }

    private fun addTasksForNativePlugin(
        project: Project,
        extension: KtlintExtension,
        sourceSetName: String,
        ktlintConfiguration: Configuration,
        gatherVariantSources: () -> List<FileCollection>
    ) {
        val sourceDirectoriesList = gatherVariantSources()
        if (sourceDirectoriesList.isNotEmpty()) {
            val checkTask = createCheckTask(
                project,
                extension,
                sourceSetName,
                ktlintConfiguration,
                sourceDirectoriesList
            )
            addKtlintCheckTaskToProjectMetaCheckTask(project, checkTask)
            setCheckTaskDependsOnKtlintCheckTask(project, checkTask)

            val ktlintSourceSetFormatTask = createFormatTask(
                project,
                extension,
                sourceSetName,
                ktlintConfiguration,
                sourceDirectoriesList
            )
            addKtlintFormatTaskToProjectMetaFormatTask(project, ktlintSourceSetFormatTask)
        }
    }

    private fun addKtlintCheckTaskToProjectMetaCheckTask(
        target: Project,
        checkTask: TaskProvider<KtlintCheckTask>
    ) {
        target.getMetaKtlintCheckTask().configure { it.dependsOn(checkTask) }
        if (target.rootProject != target) {
            target.rootProject.getMetaKtlintCheckTask().configure { it.dependsOn(checkTask) }
        }
    }

    private fun addKtlintFormatTaskToProjectMetaFormatTask(
        target: Project,
        formatTask: TaskProvider<KtlintFormatTask>
    ) {
        target.getMetaKtlintFormatTask().configure { it.dependsOn(formatTask) }
        if (target.rootProject != target) {
            target.rootProject.getMetaKtlintFormatTask().configure { it.dependsOn(formatTask) }
        }
    }

    private fun createFormatTask(
        target: Project,
        extension: KtlintExtension,
        sourceSetName: String,
        ktLintConfig: Configuration,
        kotlinSourceDirectories: Iterable<*>
    ): TaskProvider<KtlintFormatTask> {
        return target.taskHelper(sourceSetName.sourceSetFormatTaskName()) {
            description = "Runs a check against all .kt files to ensure that they are formatted according to ktlint."
            configurePluginTask(target, extension, ktLintConfig, kotlinSourceDirectories)
        }
    }

    private fun createCheckTask(
        target: Project,
        extension: KtlintExtension,
        sourceSetName: String,
        ktLintConfig: Configuration,
        kotlinSourceDirectories: Iterable<*>
    ): TaskProvider<KtlintCheckTask> {
        return target.taskHelper(sourceSetName.sourceSetCheckTaskName()) {
            description = "Runs a check against all .kt files to ensure that they are formatted according to ktlint."
            configurePluginTask(target, extension, ktLintConfig, kotlinSourceDirectories)
        }
    }

    private fun KtlintCheckTask.configurePluginTask(
        target: Project,
        extension: KtlintExtension,
        ktLintConfig: Configuration,
        kotlinSourceDirectories: Iterable<*>
    ) {
        classpath.setFrom(ktLintConfig)
        setSource(kotlinSourceDirectories)
        ktlintVersion.set(extension.version)
        verbose.set(extension.verbose)
        debug.set(extension.debug)
        android.set(extension.android)
        ignoreFailures.set(extension.ignoreFailures)
        outputToConsole.set(extension.outputToConsole)
        coloredOutput.set(extension.coloredOutput.map {
            if (target.isConsolePlain()) {
                target.logger.info("Console type is plain: disabling colored output")
                false
            } else {
                it
            }
        })
        ruleSets.set(extension.ruleSets)
        reporters.set(extension.reporters)
    }

    private fun Project.getMetaKtlintCheckTask(): TaskProvider<Task> = try {
        this.tasks.named(CHECK_PARENT_TASK_NAME)
    } catch (ignored: UnknownDomainObjectException) {
        taskHelper(CHECK_PARENT_TASK_NAME) {
            group = VERIFICATION_GROUP
            description = "Runs ktlint on all kotlin sources in this project."
        }
    }

    private fun Project.getAndroidVariantMetaKtlintCheckTask(
        variantName: String,
        multiplatformTargetName: String? = null
    ): TaskProvider<Task> {
        val taskName = "ktlint${variantName.capitalize()}${multiplatformTargetName?.capitalize() ?: ""}Check"
        return try {
            tasks.named(taskName)
        } catch (ignored: UnknownDomainObjectException) {
            taskHelper(taskName) {
                group = VERIFICATION_GROUP
                description = "Runs ktlint on all kotlin sources for android $variantName variant in this project."
            }
        }
    }

    private fun Project.getMetaKtlintFormatTask(): TaskProvider<Task> = try {
        this.tasks.named(FORMAT_PARENT_TASK_NAME)
    } catch (ignored: UnknownDomainObjectException) {
        taskHelper(FORMAT_PARENT_TASK_NAME) {
            group = FORMATTING_GROUP
            description = "Runs the ktlint formatter on all kotlin sources in this project."
        }
    }

    private fun Project.getAndroidVariantMetaKtlintFormatTask(
        variantName: String,
        multiplatformTargetName: String? = null
    ): TaskProvider<Task> {
        val taskName = "ktlint${variantName.capitalize()}${multiplatformTargetName?.capitalize() ?: ""}Format"
        return try {
            tasks.named(taskName)
        } catch (ignored: UnknownDomainObjectException) {
            taskHelper(taskName) {
                group = FORMATTING_GROUP
                description = "Runs ktlint formatter on all kotlin sources for android $variantName" +
                    " variant in this project."
            }
        }
    }

    private fun setCheckTaskDependsOnKtlintCheckTask(
        project: Project,
        ktlintCheck: TaskProvider<KtlintCheckTask>
    ) {
        try {
            project.tasks.named("check").configure { it.dependsOn(ktlintCheck) }
        } catch (e: UnknownDomainObjectException) {
            project.logger.info("Check task is not available.")
        }
    }

    /*
     * Helper functions used until Gradle Script Kotlin solidifies it's plugin API.
     */

    private inline fun <reified T : Any> Project.theHelper() =
        theHelper(T::class)

    private fun <T : Any> Project.theHelper(extensionType: KClass<T>) =
        convention.findPlugin(extensionType.java) ?: convention.getByType(extensionType.java)

    private inline fun <reified T> Convention.getPluginHelper() = getPlugin(T::class.java)

    private fun Project.isConsolePlain() = gradle.startParameter.consoleOutput == ConsoleOutput.Plain
}
