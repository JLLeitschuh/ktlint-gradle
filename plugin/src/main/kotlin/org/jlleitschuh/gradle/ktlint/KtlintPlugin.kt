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
            group = VERIFICATION_GROUP
            description = "Runs ktlint on all kotlin sources in this project."
        }

        target.tasks.register(FORMAT_PARENT_TASK_NAME, DefaultTask::class.java) {
            group = FORMATTING_GROUP
            description = "Runs the ktlint formatter on all kotlin sources in this project."
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
        project.plugins.withId("kotlin", applyKtLint(project))
        project.plugins.withId("org.jetbrains.kotlin.js", applyKtLint(project))
        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            applyKtlintMultiplatform(project)
        }
        addKtLintTasksToAndroidIfNecessary()
    }

    private fun PluginHolder.addKtLintTasksToAndroidIfNecessary() {
        // Classic API (with kotlin-android plugin)
        target.plugins.withId("org.jetbrains.kotlin.android", applyKtLintToAndroid(target))

        // New API from AGP 9+ (with built-in Kotlin plugin)
        target.pluginManager.withPlugin("com.android.base") {
            if (target.plugins.hasKotlinBaseApiPlugin) {
                target.plugins.withId(id, applyKtLintToAndroid(target))
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

    private val PluginContainer.hasKotlinBaseApiPlugin: Boolean get() =
        kotlinBaseApiPluginClass?.let(this::hasPlugin) ?: false

    private fun applyKtlintMultiplatform(project: Project) {
        val multiplatformExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        multiplatformExtension.sourceSets.configureEach {
            val checkTask = createCheckTask(project, name, kotlin)
            val generateReportsCheckTask =
                GenerateReportsTask.registerCheckReportTaskForSourceSet(project, checkTask, name)

            addGenerateReportsTaskToProjectMetaCheckTask(project, generateReportsCheckTask)
            setCheckTaskDependsOnGenerateReportsTask(project, generateReportsCheckTask)

            val formatTask = createFormatTask(project, name, kotlin)
            val generateReportsFormatTask =
                GenerateReportsTask.registerFormatReportTaskForSourceSet(project, formatTask, name)
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
            sourceSets.configureEach {
                val kotlinSourceDirectories = this.kotlin
                val checkTask = createCheckTask(project, this.name, kotlinSourceDirectories)
                val generateReportsCheckTask =
                    GenerateReportsTask.registerCheckReportTaskForSourceSet(project, checkTask, this.name)
                addGenerateReportsTaskToProjectMetaCheckTask(project, generateReportsCheckTask)
                setCheckTaskDependsOnGenerateReportsTask(project, generateReportsCheckTask)

                val formatTask = createFormatTask(project, this.name, kotlinSourceDirectories)
                val generateReportsFormatTask =
                    GenerateReportsTask.registerFormatReportTaskForSourceSet(project, formatTask, this.name)

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

    internal class PluginHolder(
        val target: Project
    ) {
        val extension = target.plugins.apply(KtlintBasePlugin::class.java).extension


        val ktlintConfiguration: Configuration = createKtlintConfiguration(target, extension)

        val ktlintReporterConfiguration: Configuration = createKtLintReporterConfiguration(target, extension, ktlintConfiguration)

    }
}
