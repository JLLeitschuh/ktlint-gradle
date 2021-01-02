package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jlleitschuh.gradle.ktlint.android.applyKtLintToAndroid

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
        target.plugins.withId("org.jetbrains.kotlin.js", applyKtLint())
        target.plugins.withId("kotlin-android", applyKtLintToAndroid())
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
            if (kotlinTarget.platformType == KotlinPlatformType.androidJvm) {
                applyKtLintToAndroid()
            }
        }
    }

    private fun PluginHolder.applyKtLint(): (Plugin<in Any>) -> Unit = {
        target.extensions.configure<KotlinProjectExtension>("kotlin") { extension ->
            extension.sourceSets.all { sourceSet ->
                val kotlinSourceDirectories = sourceSet.kotlin.sourceDirectories
                val checkTask = createCheckTask(
                    this,
                    sourceSet.name,
                    kotlinSourceDirectories
                )

                addKtlintCheckTaskToProjectMetaCheckTask(checkTask)
                setCheckTaskDependsOnKtlintCheckTask(target, checkTask)

                val ktlintSourceSetFormatTask = createFormatTask(
                    this,
                    sourceSet.name,
                    kotlinSourceDirectories
                )

                addKtlintFormatTaskToProjectMetaFormatTask(ktlintSourceSetFormatTask)
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
        val ktlintReporterConfiguration by lazy(LazyThreadSafetyMode.NONE) {
            createKtlintReporterConfiguration(target, extension)
        }
        val loadRuleSetsTask = createLoadRuleSetsTask(this)
        val loadReportersTask = createLoadReportersTask(this)
    }
}
