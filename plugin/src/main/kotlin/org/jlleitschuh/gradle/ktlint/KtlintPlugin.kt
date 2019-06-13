package org.jlleitschuh.gradle.ktlint

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
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
@Suppress("UnstableApiUsage")
open class KtlintPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val holder = PluginHolder(target)
        // Apply the idea plugin
        target.plugins.apply(KtlintIdeaPlugin::class.java)

        holder.addKtLintTasksToKotlinPlugin()
        holder.addKotlinScriptTasks()
        holder.addGitHookTasks()
    }

    private fun PluginHolder.addKtLintTasksToKotlinPlugin() {
        target.plugins.withId("kotlin", applyKtLint())
        target.plugins.withId("kotlin2js", applyKtLint())
        target.plugins.withId("kotlin-platform-common", applyKtLint())
        target.plugins.withId("kotlin-android", applyKtLintToAndroid())
        target.plugins.withId("konan", applyKtLintKonanNative())
        target.plugins.withId(
            "org.jetbrains.kotlin.native",
            applyKtLintNative()
        )
        target.plugins.withId(
            "org.jetbrains.kotlin.multiplatform",
            applyKtlintMultiplatform()
        )
    }

    private fun PluginHolder.applyKtlintMultiplatform(): (Plugin<in Any>) -> Unit = {
        val multiplatformExtension = target.extensions.getByType(KotlinMultiplatformExtension::class.java)

        multiplatformExtension.sourceSets.all { sourceSet ->
            val checkTask = createCheckTask(
                this,
                sourceSet.name,
                sourceSet.kotlin.sourceDirectories
            )

            addKtlintCheckTaskToProjectMetaCheckTask(checkTask)
            setCheckTaskDependsOnKtlintCheckTask(target, checkTask)

            val ktlintSourceSetFormatTask = createFormatTask(
                this,
                sourceSet.name,
                sourceSet.kotlin.sourceDirectories
            )

            addKtlintFormatTaskToProjectMetaFormatTask(ktlintSourceSetFormatTask)
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

    private fun PluginHolder.applyKtLint(): (Plugin<in Any>) -> Unit {
        return {
            val sourceSets = target.theHelper<JavaPluginConvention>().sourceSets

            sourceSets.all { sourceSet ->
                val kotlinSourceSet: SourceDirectorySet = (sourceSet as HasConvention)
                    .convention
                    .getPluginHelper<KotlinSourceSet>()
                    .kotlin
                val checkTask = createCheckTask(
                    this,
                    sourceSet.name,
                    kotlinSourceSet.sourceDirectories
                )

                addKtlintCheckTaskToProjectMetaCheckTask(checkTask)
                setCheckTaskDependsOnKtlintCheckTask(target, checkTask)

                val ktlintSourceSetFormatTask = createFormatTask(
                    this,
                    sourceSet.name,
                    kotlinSourceSet.sourceDirectories
                )

                addKtlintFormatTaskToProjectMetaFormatTask(ktlintSourceSetFormatTask)
            }
        }
    }

    private fun PluginHolder.applyKtLintToAndroid(): (Plugin<in Any>) -> Unit {
        return {
            fun createTasks(
                sourceSetName: String,
                sources: FileCollection
            ) {
                val checkTask = createCheckTask(
                    this,
                    sourceSetName,
                    sources
                )

                addKtlintCheckTaskToProjectMetaCheckTask(checkTask)
                setCheckTaskDependsOnKtlintCheckTask(target, checkTask)

                val ktlintSourceSetFormatTask = createFormatTask(
                    this,
                    sourceSetName,
                    sources
                )

                addKtlintFormatTaskToProjectMetaFormatTask(ktlintSourceSetFormatTask)
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

    private fun PluginHolder.applyKtLintKonanNative(): (Plugin<in Any>) -> Unit {
        return {
            val compileTargets = target.theHelper<KonanExtension>().targets
            target.theHelper<KonanArtifactContainer>().whenObjectAdded { buildConfig ->
                addTasksForNativePlugin(buildConfig.name) {
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

    private fun PluginHolder.applyKtLintNative(): (Plugin<in Any>) -> Unit {
        return {
            target.components.withType(KotlinNativeComponent::class.java) { component ->
                addTasksForNativePlugin(component.name) {
                    component.konanTargets.get()
                        .fold(initial = emptyList()) { acc, nativeTarget ->
                            acc + listOf(component.sources.getAllSources(nativeTarget))
                        }
                }
            }
        }
    }

    private fun PluginHolder.addTasksForNativePlugin(
        sourceSetName: String,
        gatherVariantSources: () -> List<FileCollection>
    ) {
        val sourceDirectoriesList = gatherVariantSources()
        if (sourceDirectoriesList.isNotEmpty()) {
            val checkTask = createCheckTask(
                this,
                sourceSetName,
                sourceDirectoriesList
            )
            addKtlintCheckTaskToProjectMetaCheckTask(checkTask)
            setCheckTaskDependsOnKtlintCheckTask(target, checkTask)

            val ktlintSourceSetFormatTask = createFormatTask(
                this,
                sourceSetName,
                sourceDirectoriesList
            )
            addKtlintFormatTaskToProjectMetaFormatTask(ktlintSourceSetFormatTask)
        }
    }

    private fun PluginHolder.addKtlintCheckTaskToProjectMetaCheckTask(
        checkTask: TaskProvider<KtlintCheckTask>
    ) {
        metaKtlintCheckTask.configure { it.dependsOn(checkTask) }
    }

    private fun PluginHolder.addKtlintFormatTaskToProjectMetaFormatTask(
        formatTask: TaskProvider<KtlintFormatTask>
    ) {
        metaKtlintFormatTask.configure { it.dependsOn(formatTask) }
    }

    private fun createFormatTask(
        pluginHolder: PluginHolder,
        sourceSetName: String,
        kotlinSourceDirectories: Iterable<*>
    ): TaskProvider<KtlintFormatTask> {
        return pluginHolder.target.registerTask(sourceSetName.sourceSetFormatTaskName()) {
            description = "Runs a check against all .kt files to ensure that they are formatted according to ktlint."
            configurePluginTask(pluginHolder) {
                setSource(kotlinSourceDirectories)
            }
        }
    }

    private fun createCheckTask(
        pluginHolder: PluginHolder,
        sourceSetName: String,
        kotlinSourceDirectories: Iterable<*>
    ): TaskProvider<KtlintCheckTask> {
        return pluginHolder.target.registerTask(sourceSetName.sourceSetCheckTaskName()) {
            description = "Runs a check against all .kt files to ensure that they are formatted according to ktlint."
            configurePluginTask(pluginHolder) {
                setSource(kotlinSourceDirectories)
            }
        }
    }

    private fun BaseKtlintCheckTask.configurePluginTask(
        pluginHolder: PluginHolder,
        additionalTaskConfig: BaseKtlintCheckTask.() -> Unit
    ) {
        classpath.setFrom(pluginHolder.ktlintConfiguration)
        ktlintVersion.set(pluginHolder.extension.version)
        verbose.set(pluginHolder.extension.verbose)
        additionalEditorconfigFile.set(pluginHolder.extension.additionalEditorconfigFile)
        debug.set(pluginHolder.extension.debug)
        ignoreFailures.set(pluginHolder.extension.ignoreFailures)
        outputToConsole.set(pluginHolder.extension.outputToConsole)
        coloredOutput.set(pluginHolder.extension.coloredOutput.map {
            if (pluginHolder.target.isConsolePlain()) {
                pluginHolder.target.logger.info("Console type is plain: disabling colored output")
                false
            } else {
                it
            }
        })
        ruleSets.set(pluginHolder.extension.ruleSets)
        ruleSetsClasspath.setFrom(pluginHolder.ktlintRulesetConfiguration)
        reporters.set(pluginHolder.extension.reporters)
        android.set(pluginHolder.extension.android)
        enableExperimentalRules.set(pluginHolder.extension.enableExperimentalRules)

        additionalTaskConfig()
    }

    private fun setCheckTaskDependsOnKtlintCheckTask(
        project: Project,
        ktlintCheck: TaskProvider<KtlintCheckTask>
    ) {
        project.plugins.withType(LifecycleBasePlugin::class.java) {
            project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { task ->
                task.dependsOn(ktlintCheck)
            }
        }
    }

    private fun PluginHolder.addKotlinScriptTasks() {
        val projectDirectoryScriptFiles = target.fileTree(target.projectDir)
        projectDirectoryScriptFiles.include("*.kts")

        val checkTask = createKotlinScriptCheckTask(this, projectDirectoryScriptFiles)
        addKtlintCheckTaskToProjectMetaCheckTask(checkTask)
        setCheckTaskDependsOnKtlintCheckTask(target, checkTask)

        val formatTask = createKotlinScriptFormatTask(this, projectDirectoryScriptFiles)
        addKtlintFormatTaskToProjectMetaFormatTask(formatTask)
    }

    private fun createKotlinScriptCheckTask(
        pluginHolder: PluginHolder,
        projectScriptFiles: FileTree
    ): TaskProvider<KtlintCheckTask> {
        return pluginHolder.target.registerTask(KOTLIN_SCRIPT_CHECK_TASK) {
            description = "Runs a check against all .kts files in project directory " +
                "to ensure that they are formatted according to ktlint."
            configurePluginTask(pluginHolder) {
                source = projectScriptFiles
            }
        }
    }

    private fun createKotlinScriptFormatTask(
        pluginHolder: PluginHolder,
        projectScriptFiles: FileTree
    ): TaskProvider<KtlintFormatTask> {
        return pluginHolder.target.registerTask(KOTLIN_SCRIPT_FORMAT_TASK) {
            description = "Format all .kts files in project directory " +
                "to ensure that they are formatted according to ktlint."
            configurePluginTask(pluginHolder) {
                source = projectScriptFiles
            }
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

    internal class PluginHolder(
        val target: Project
    ) {
        val extension = target.plugins.apply(KtlintBasePlugin::class.java).extension

        val metaKtlintCheckTask: TaskProvider<Task> by lazy {
            target.registerTask<Task>(CHECK_PARENT_TASK_NAME) {
                group = VERIFICATION_GROUP
                description = "Runs ktlint on all kotlin sources in this project."
            }
        }

        val metaKtlintFormatTask: TaskProvider<Task> by lazy {
            target.registerTask<Task>(FORMAT_PARENT_TASK_NAME) {
                group = FORMATTING_GROUP
                description = "Runs the ktlint formatter on all kotlin sources in this project."
            }
        }

        val ktlintConfiguration by lazy(LazyThreadSafetyMode.NONE) { createKtlintConfiguration(target, extension) }
        val ktlintRulesetConfiguration = createKtlintRulesetConfiguration(target)
    }
}

/**
 * This is not scoped inside of [KtlintPlugin] because of these issues:
 *  - https://github.com/JLLeitschuh/ktlint-gradle/issues/201
 *  - https://github.com/gradle/gradle/issues/8411
 */
private fun BaseExtension.addVariantsMetaTasks(
    target: Project,
    multiplatformTargetName: String? = null
) {
    variants?.all { variant ->
        val variantCheckTask = target.createAndroidVariantMetaKtlintCheckTask(
            variant.name,
            multiplatformTargetName
        )
        val variantFormatTask = target.createAndroidVariantMetaKtlintFormatTask(
            variant.name,
            multiplatformTargetName
        )
        variant.sourceSets.forEach { sourceSet ->
            val sourceSetName = "${multiplatformTargetName?.capitalize() ?: ""}${sourceSet.name.capitalize()}"
            variantCheckTask.configure { it.dependsOn(sourceSetName.sourceSetCheckTaskName()) }
            variantFormatTask.configure { it.dependsOn(sourceSetName.sourceSetFormatTaskName()) }
        }
    }
}

private fun Project.createAndroidVariantMetaKtlintCheckTask(
    variantName: String,
    multiplatformTargetName: String? = null
): TaskProvider<Task> = registerTask(variantName.androidVariantMetaCheckTaskName(multiplatformTargetName)) {
    group = VERIFICATION_GROUP
    description = "Runs ktlint on all kotlin sources for android $variantName variant in this project."
}

private fun Project.createAndroidVariantMetaKtlintFormatTask(
    variantName: String,
    multiplatformTargetName: String? = null
): TaskProvider<Task> = registerTask(variantName.androidVariantMetaFormatTaskName(multiplatformTargetName)) {
    group = FORMATTING_GROUP
    description = "Runs ktlint formatter on all kotlin sources for android $variantName" +
        " variant in this project."
}
