package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jlleitschuh.gradle.ktlint.android.applyKtLintToAndroid
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask

/**
 * Plugin that provides a wrapper over the `ktlint` project.
 */
@Suppress("UnstableApiUsage")
open class KtlintPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val holder = PluginHolder(target)
        holder.addKotlinScriptTasks()
        holder.addKtLintTasksToKotlinPlugin()
        holder.addGenerateBaselineTask()
        holder.addGitHookTasks()
    }

    private fun PluginHolder.addKtLintTasksToKotlinPlugin() {
        target.plugins.withId("kotlin", applyKtLint())
        target.plugins.withId("org.jetbrains.kotlin.js", applyKtLint())
        target.plugins.withId("kotlin-android", applyKtLintToAndroid())
        target.plugins.withId(
            "org.jetbrains.kotlin.multiplatform",
            applyKtlintMultiplatform()
        )
    }

    private fun PluginHolder.applyKtlintMultiplatform(): (Plugin<in Any>) -> Unit = {
        val multiplatformExtension = target.extensions.getByType(KotlinMultiplatformExtension::class.java)

        multiplatformExtension.sourceSets.all(fun(sourceSet) {
            val checkTask = createCheckTask(
                this,
                sourceSet.name,
                sourceSet.kotlin.sourceDirectories
            )
            val generateReportsCheckTask = createGenerateReportsTask(
                this,
                checkTask,
                GenerateReportsTask.LintType.CHECK,
                sourceSet.name
            )

            addGenerateReportsTaskToProjectMetaCheckTask(generateReportsCheckTask)
            setCheckTaskDependsOnGenerateReportsTask(generateReportsCheckTask)

            val formatTask = createFormatTask(
                this,
                sourceSet.name,
                sourceSet.kotlin.sourceDirectories
            )
            val generateReportsFormatTask = createGenerateReportsTask(
                this,
                formatTask,
                GenerateReportsTask.LintType.FORMAT,
                sourceSet.name
            )

            addGenerateReportsTaskToProjectMetaFormatTask(generateReportsFormatTask)
        })

        multiplatformExtension.targets.all(fun(kotlinTarget) {
            if (kotlinTarget.platformType == KotlinPlatformType.androidJvm) {
                applyKtLintToAndroid()
            }
        })
    }

    private fun PluginHolder.applyKtLint(): (Plugin<in Any>) -> Unit = {
        target.extensions.configure(KotlinProjectExtension::class.java) {
            sourceSets.all(fun(sourceSet) {
                val kotlinSourceDirectories = sourceSet.kotlin.sourceDirectories
                val checkTask = createCheckTask(
                    this@applyKtLint,
                    sourceSet.name,
                    kotlinSourceDirectories
                )
                val generateReportsCheckTask = createGenerateReportsTask(
                    this@applyKtLint,
                    checkTask,
                    GenerateReportsTask.LintType.CHECK,
                    sourceSet.name
                )

                addGenerateReportsTaskToProjectMetaCheckTask(generateReportsCheckTask)
                setCheckTaskDependsOnGenerateReportsTask(generateReportsCheckTask)

                val formatTask = createFormatTask(
                    this@applyKtLint,
                    sourceSet.name,
                    kotlinSourceDirectories
                )
                val generateReportsFormatTask = createGenerateReportsTask(
                    this@applyKtLint,
                    formatTask,
                    GenerateReportsTask.LintType.FORMAT,
                    sourceSet.name
                )

                addGenerateReportsTaskToProjectMetaFormatTask(generateReportsFormatTask)
            })
        }
    }

    private fun PluginHolder.addKotlinScriptTasks() {
        val projectDirectoryScriptFiles = target.fileTree(target.projectDir)
        projectDirectoryScriptFiles.include("*.kts")

        val checkTask = createKotlinScriptCheckTask(this, projectDirectoryScriptFiles)
        val generateReportsCheckTask = createKotlinScriptGenerateReportsTask(
            this,
            checkTask,
            GenerateReportsTask.LintType.CHECK
        )
        addGenerateReportsTaskToProjectMetaCheckTask(generateReportsCheckTask)
        setCheckTaskDependsOnGenerateReportsTask(generateReportsCheckTask)

        val formatTask = createKotlinScriptFormatTask(this, projectDirectoryScriptFiles)
        val generateReportsFormatTask = createKotlinScriptGenerateReportsTask(
            this,
            formatTask,
            GenerateReportsTask.LintType.FORMAT
        )
        addGenerateReportsTaskToProjectMetaFormatTask(generateReportsFormatTask)
    }

    private fun PluginHolder.addGenerateBaselineTask() {
        createGenerateBaselineTask(
            this,
            target.tasks.withType(KtLintCheckTask::class.java)
        )
    }

    internal class PluginHolder(
        val target: Project
    ) {
        val extension = target.plugins.apply(KtlintBasePlugin::class.java).extension

        val metaKtlintCheckTask: TaskProvider<Task> by lazy {
            target.registerTask(CHECK_PARENT_TASK_NAME) {
                group = VERIFICATION_GROUP
                description = "Runs ktlint on all kotlin sources in this project."
            }
        }

        val metaKtlintFormatTask: TaskProvider<Task> by lazy {
            target.registerTask(FORMAT_PARENT_TASK_NAME) {
                group = FORMATTING_GROUP
                description = "Runs the ktlint formatter on all kotlin sources in this project."
            }
        }

        val ktlintConfiguration: Configuration = createKtlintConfiguration(target, extension)
        val ktlintRulesetConfiguration: Configuration = createKtlintRulesetConfiguration(
            target,
            ktlintConfiguration,
            extension
        )
        val ktlintReporterConfiguration: Configuration = createKtLintReporterConfiguration(target, extension, ktlintConfiguration)
        val ktlintBaselineReporterConfiguration: Configuration = createKtLintBaselineReporterConfiguration(
            target,
            extension,
            ktlintConfiguration
        )
        val loadReportersTask = createLoadReportersTask(this)
    }
}
