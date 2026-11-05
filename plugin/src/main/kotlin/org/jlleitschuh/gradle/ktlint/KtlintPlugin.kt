package org.jlleitschuh.gradle.ktlint

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
        target.tasks.register(CHECK_PARENT_TASK_NAME) {
            group = VERIFICATION_GROUP
            description = "Runs ktlint on all kotlin sources in this project."
        }
        target.tasks.register(FORMAT_PARENT_TASK_NAME) {
            group = FORMATTING_GROUP
            description = "Runs the ktlint formatter on all kotlin sources in this project."
        }
        val holder = PluginHolder(target)
        holder.addKotlinScriptTasks()
        holder.addKtLintTasksToKotlinPlugin()
        createKtLintReporterConfiguration(target, holder.extension, holder.ktlintConfiguration)
        createKtLintBaselineReporterConfiguration(target, holder.ktlintConfiguration)
        createKtlintRulesetConfiguration(target, holder.ktlintConfiguration, holder.extension)
        LoadReportersTask.register(target)
        createGenerateBaselineTask(target, target.tasks.withType(KtLintCheckTask::class.java))
        addGitHookTasks(target)
    }

    private fun PluginHolder.addKtLintTasksToKotlinPlugin() {
        target.plugins.withId("kotlin", applyKtLint())
        target.plugins.withId("org.jetbrains.kotlin.js", applyKtLint())
        target.plugins.withId(
            "org.jetbrains.kotlin.multiplatform",
            applyKtlintMultiplatform()
        )
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

    private val PluginContainer.hasKotlinBaseApiPlugin: Boolean
        get() =
            kotlinBaseApiPluginClass?.let(this::hasPlugin) ?: false

    private fun PluginHolder.applyKtlintMultiplatform(): (Plugin<in Any>) -> Unit = {
        val multiplatformExtension = target.extensions.getByType(KotlinMultiplatformExtension::class.java)

        multiplatformExtension.sourceSets.all(fun(sourceSet) {
            val checkTask = createCheckTask(
                target,
                sourceSet.name,
                sourceSet.kotlin
            )
            val generateReportsCheckTask =
                GenerateReportsTask.registerCheckReportTaskForSourceSet(target, checkTask, sourceSet.name)

            addGenerateReportsTaskToProjectMetaCheckTask(target, generateReportsCheckTask)
            setCheckTaskDependsOnGenerateReportsTask(target, generateReportsCheckTask)

            val formatTask = createFormatTask(
                target,
                sourceSet.name,
                sourceSet.kotlin
            )
            val generateReportsFormatTask =
                GenerateReportsTask.registerFormatReportTaskForSourceSet(target, formatTask, sourceSet.name)

            addGenerateReportsTaskToProjectMetaFormatTask(target, generateReportsFormatTask)
        })

        multiplatformExtension.targets.all(fun(kotlinTarget) {
            if (kotlinTarget.platformType == KotlinPlatformType.androidJvm) {
                applyKtLintToAndroid(target)
            }
        })
    }

    private fun PluginHolder.applyKtLint(): (Plugin<in Any>) -> Unit = {
        target.extensions.configure(KotlinProjectExtension::class.java) {
            sourceSets.all(fun(sourceSet) {
                val checkTask = createCheckTask(
                    target,
                    sourceSet.name,
                    sourceSet.kotlin
                )
                val generateReportsCheckTask =
                    GenerateReportsTask.registerCheckReportTaskForSourceSet(target, checkTask, sourceSet.name)

                addGenerateReportsTaskToProjectMetaCheckTask(target, generateReportsCheckTask)
                setCheckTaskDependsOnGenerateReportsTask(target, generateReportsCheckTask)

                val formatTask = createFormatTask(
                    target,
                    sourceSet.name,
                    sourceSet.kotlin
                )
                val generateReportsFormatTask =
                    GenerateReportsTask.registerFormatReportTaskForSourceSet(target, formatTask, sourceSet.name)

                addGenerateReportsTaskToProjectMetaFormatTask(target, generateReportsFormatTask)
            })
        }
    }

    private fun PluginHolder.addKotlinScriptTasks() {
        val checkTask = createKotlinScriptCheckTask(target)
        val generateReportsCheckTask = GenerateReportsTask.registerCheckReportTaskForScript(target, checkTask)

        addGenerateReportsTaskToProjectMetaCheckTask(target, generateReportsCheckTask)
        setCheckTaskDependsOnGenerateReportsTask(target, generateReportsCheckTask)

        val formatTask = createKotlinScriptFormatTask(target)
        val generateReportsFormatTask = GenerateReportsTask.registerFormatReportTaskForScript(target, formatTask)
        addGenerateReportsTaskToProjectMetaFormatTask(target, generateReportsFormatTask)
    }

    internal class PluginHolder(
        val target: Project
    ) {
        val extension = target.plugins.apply(KtlintBasePlugin::class.java).extension
        val ktlintConfiguration: Configuration = createKtlintConfiguration(target, extension)
    }
}
