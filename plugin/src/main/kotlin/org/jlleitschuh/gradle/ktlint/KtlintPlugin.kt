package org.jlleitschuh.gradle.ktlint

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.PluginContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jlleitschuh.gradle.ktlint.android.applyKtLintToAndroid
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.LoadReportersTask

/**
 * Plugin that provides a wrapper over the `ktlint` project.
 */
open class KtlintPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.tasks.register(CHECK_PARENT_TASK_NAME, DefaultTask::class.java) {
            it.group = VERIFICATION_GROUP
            it.description = "Runs ktlint on all kotlin sources in this project."
        }

        target.tasks.register(FORMAT_PARENT_TASK_NAME, DefaultTask::class.java) {
            it.group = FORMATTING_GROUP
            it.description = "Runs the ktlint formatter on all kotlin sources in this project."
        }
        target.plugins.apply(KtlintBasePlugin::class.java)
        val extension = target.extensions.getByType(KtlintExtension::class.java)
        val ktlintConfiguration: Configuration = createKtlintConfiguration(target, extension)
        createKtlintRulesetConfiguration(target, ktlintConfiguration, extension)
        createKtLintReporterConfiguration(target, extension, ktlintConfiguration)
        createKtLintBaselineReporterConfiguration(target, ktlintConfiguration)
        LoadReportersTask.register(target)
        target.plugins.withId("kotlin") { applyKtLint(target) }
        target.plugins.withId("org.jetbrains.kotlin.js") { applyKtLint(target) }
        target.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            applyKtlintMultiplatform(target)
        }
        addKotlinScriptTasks(target)
        addKtLintTasksToKotlinPlugin(target)
        createGenerateBaselineTask(
            target,
            target.tasks.withType(KtLintCheckTask::class.java)
        )
        addGitHookTasks(target)
    }

    private fun addKtLintTasksToKotlinPlugin(project: Project) {
        addKtLintTasksToAndroidIfNecessary(project)
    }

    private fun addKtLintTasksToAndroidIfNecessary(project: Project) {
        // Classic API (with kotlin-android plugin)
        project.plugins.withId("org.jetbrains.kotlin.android", applyKtLintToAndroid(project))

        // New API from AGP 9+ (with built-in Kotlin plugin)
        project.pluginManager.withPlugin("com.android.base") {
            if (project.plugins.hasKotlinBaseApiPlugin) {
                project.plugins.withId(it.id, applyKtLintToAndroid(project))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val kotlinBaseApiPluginClass by lazy {
        try {
            // TODO: After raising the baseline Kotlin version to 1.7.20 or newer,
            //  replace this reflective lookup with the actual class
            Class.forName("org.jetbrains.kotlin.gradle.plugin.KotlinBaseApiPlugin")
                as? Class<out Plugin<*>>
        } catch (_: ClassNotFoundException) {
            null
        }
    }

    private val PluginContainer.hasKotlinBaseApiPlugin: Boolean
        get() =
            kotlinBaseApiPluginClass?.let(this::hasPlugin) ?: false

    private fun applyKtlintMultiplatform(project: Project) {
        val multiplatformExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        multiplatformExtension.sourceSets.configureEach { sourceSet ->
            val checkTask = createCheckTask(project, sourceSet.name, sourceSet.kotlin)
            val generateReportsCheckTask =
                GenerateReportsTask.registerCheckReportTaskForSourceSet(project, checkTask, sourceSet.name)

            addGenerateReportsTaskToProjectMetaCheckTask(project, generateReportsCheckTask)
            setCheckTaskDependsOnGenerateReportsTask(project, generateReportsCheckTask)

            val formatTask = createFormatTask(project, sourceSet.name, sourceSet.kotlin)
            val generateReportsFormatTask =
                GenerateReportsTask.registerFormatReportTaskForSourceSet(project, formatTask, sourceSet.name)
            addGenerateReportsTaskToProjectMetaFormatTask(project, generateReportsFormatTask)
        }

        multiplatformExtension.targets.all(fun(kotlinTarget) {
            if (kotlinTarget.platformType == KotlinPlatformType.androidJvm) {
                applyKtLintToAndroid(project)
            }
        })
    }

    private fun applyKtLint(project: Project) {
        project.extensions.configure(KotlinProjectExtension::class.java) {
            it.sourceSets.configureEach { sourceSet ->
                val kotlinSourceDirectories = sourceSet.kotlin
                val checkTask = createCheckTask(project, sourceSet.name, kotlinSourceDirectories)
                val generateReportsCheckTask =
                    GenerateReportsTask.registerCheckReportTaskForSourceSet(project, checkTask, sourceSet.name)
                addGenerateReportsTaskToProjectMetaCheckTask(project, generateReportsCheckTask)
                setCheckTaskDependsOnGenerateReportsTask(project, generateReportsCheckTask)

                val formatTask = createFormatTask(project, sourceSet.name, kotlinSourceDirectories)
                val generateReportsFormatTask =
                    GenerateReportsTask.registerFormatReportTaskForSourceSet(project, formatTask, sourceSet.name)

                addGenerateReportsTaskToProjectMetaFormatTask(project, generateReportsFormatTask)
            }
        }
    }

    private fun addKotlinScriptTasks(project: Project) {
        val checkTask = createKotlinScriptCheckTask(project)
        val generateReportsCheckTask = GenerateReportsTask.registerCheckReportTaskForScript(project, checkTask)

        addGenerateReportsTaskToProjectMetaCheckTask(project, generateReportsCheckTask)
        setCheckTaskDependsOnGenerateReportsTask(project, generateReportsCheckTask)

        val formatTask = createKotlinScriptFormatTask(project)
        val generateReportsFormatTask =
            GenerateReportsTask.registerFormatReportTaskForScript(project, formatTask)
        addGenerateReportsTaskToProjectMetaFormatTask(project, generateReportsFormatTask)
    }
}
