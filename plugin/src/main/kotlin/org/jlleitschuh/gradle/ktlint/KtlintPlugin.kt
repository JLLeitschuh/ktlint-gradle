package org.jlleitschuh.gradle.ktlint

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.JavaPluginConvention
import org.jetbrains.kotlin.gradle.plugin.KonanArtifactContainer
import org.jetbrains.kotlin.gradle.plugin.KonanExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeComponent
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileTask
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
    }

    private fun applyKtLint(
        target: Project,
        extension: KtlintExtension
    ): (Plugin<in Any>) -> Unit {
        return { _ ->
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
        return { _ ->
            val ktLintConfig = createConfiguration(target, extension)

            fun createTasks(
                fullVariantName: String,
                sources: FileCollection
            ) {
                val checkTask = createCheckTask(
                    target,
                    extension,
                    fullVariantName,
                    ktLintConfig,
                    sources
                )

                addKtlintCheckTaskToProjectMetaCheckTask(target, checkTask)
                setCheckTaskDependsOnKtlintCheckTask(target, checkTask)

                val ktlintSourceSetFormatTask = createFormatTask(
                    target,
                    extension,
                    fullVariantName,
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
            fun getPluginConfigureAction(): (Plugin<Any>) -> Unit = { _ ->
                target.extensions.configure(BaseExtension::class.java) { ext ->
                    ext.sourceSets { sourceSet ->
                        sourceSet.all {
                            createTasks(it.name, target.files(it.java.srcDirs))
                        }
                    }
                }
            }

            target.plugins.withId("com.android.application", getPluginConfigureAction())
            target.plugins.withId("com.android.library", getPluginConfigureAction())
            target.plugins.withId("com.android.instantapp", getPluginConfigureAction())
            target.plugins.withId("com.android.feature", getPluginConfigureAction())
            target.plugins.withId("com.android.test", getPluginConfigureAction())
        }
    }

    private fun applyKtLintKonanNative(
        project: Project,
        extension: KtlintExtension
    ): (Plugin<in Any>) -> Unit {
        return { _ ->
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
        return { _ ->
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
        extension: KtlintExtension,
        sourceSetName: String,
        ktLintConfig: Configuration,
        kotlinSourceDirectories: Iterable<*>
    ): Task {
        return target.taskHelper<KtlintFormatTask>("ktlint${sourceSetName.capitalize()}Format") {
            group = FORMATTING_GROUP
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
    ): Task {
        return target.taskHelper<KtlintCheckTask>("ktlint${sourceSetName.capitalize()}Check") {
            group = VERIFICATION_GROUP
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
        sourceDirectories.setFrom(kotlinSourceDirectories)
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

    private fun Project.isConsolePlain() = gradle.startParameter.consoleOutput == ConsoleOutput.Plain
}
