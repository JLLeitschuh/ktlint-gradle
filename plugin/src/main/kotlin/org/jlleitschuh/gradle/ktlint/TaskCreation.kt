package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.util.PatternSet
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.GenerateBaselineTask
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask.Companion.FORMAT_KOTLIN_SCRIPT_TASK_NAME
import org.jlleitschuh.gradle.ktlint.tasks.LoadReportersTask

internal fun KtlintPlugin.PluginHolder.addGenerateReportsTaskToProjectMetaCheckTask(
    generatesReportsTask: TaskProvider<GenerateReportsTask>
) {
    metaKtlintCheckTask.configure { dependsOn(generatesReportsTask) }
}

internal fun KtlintPlugin.PluginHolder.addGenerateReportsTaskToProjectMetaFormatTask(
    generateReportsTask: TaskProvider<GenerateReportsTask>
) {
    metaKtlintFormatTask.configure { dependsOn(generateReportsTask) }
}

internal fun createFormatTask(
    pluginHolder: KtlintPlugin.PluginHolder,
    sourceSetName: String,
    kotlinSourceDirectories: Iterable<*>
): TaskProvider<KtLintFormatTask> = pluginHolder
    .target
    .registerTask(
        KtLintFormatTask.buildTaskNameForSourceSet(sourceSetName),
        PatternSet()
    ) {
        mustRunAfter(project.tasks.named(FORMAT_KOTLIN_SCRIPT_TASK_NAME))

        description = KtLintFormatTask.buildDescription(".kt")
        configureBaseCheckTask(pluginHolder) {
            setSource(kotlinSourceDirectories)
        }
    }

internal fun createCheckTask(
    pluginHolder: KtlintPlugin.PluginHolder,
    sourceSetName: String,
    kotlinSourceDirectories: Iterable<*>
): TaskProvider<KtLintCheckTask> = pluginHolder
    .target
    .registerTask(
        KtLintCheckTask.buildTaskNameForSourceSet(sourceSetName),
        PatternSet()
    ) {
        description = KtLintCheckTask.buildDescription(".kt")
        configureBaseCheckTask(pluginHolder) {
            setSource(kotlinSourceDirectories)
        }
    }

internal fun createKotlinScriptCheckTask(
    pluginHolder: KtlintPlugin.PluginHolder,
    projectScriptFiles: FileTree
): TaskProvider<KtLintCheckTask> = pluginHolder
    .target
    .registerTask(
        KtLintCheckTask.KOTLIN_SCRIPT_TASK_NAME,
        PatternSet()
    ) {
        description = KtLintCheckTask.buildDescription(".kts")
        configureBaseCheckTask(pluginHolder) {
            setSource(projectScriptFiles)
        }
    }

internal fun createKotlinScriptFormatTask(
    pluginHolder: KtlintPlugin.PluginHolder,
    projectScriptFiles: FileTree
): TaskProvider<KtLintFormatTask> = pluginHolder
    .target
    .registerTask(
        FORMAT_KOTLIN_SCRIPT_TASK_NAME,
        PatternSet()
    ) {
        description = KtLintFormatTask.buildDescription(".kts")
        configureBaseCheckTask(pluginHolder) {
            setSource(projectScriptFiles)
        }
    }

internal fun KtlintPlugin.PluginHolder.setCheckTaskDependsOnGenerateReportsTask(
    generateReportsTask: TaskProvider<GenerateReportsTask>
) {
    target.plugins.withType(LifecycleBasePlugin::class.java) {
        target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
            dependsOn(generateReportsTask)
        }
    }
}

private fun BaseKtLintCheckTask.configureBaseCheckTask(
    pluginHolder: KtlintPlugin.PluginHolder,
    additionalTaskConfig: BaseKtLintCheckTask.() -> Unit
) {
    val loadReportersTask = pluginHolder.target.tasks.named(LoadReportersTask.TASK_NAME, LoadReportersTask::class.java)
    ktLintClasspath.setFrom(pluginHolder.ktlintConfiguration)
    ktLintVersion.set(pluginHolder.extension.version)
    additionalEditorconfig.set(pluginHolder.extension.additionalEditorconfig)
    debug.set(pluginHolder.extension.debug)
    ruleSetsClasspath.setFrom(pluginHolder.ktlintRulesetConfiguration)
    android.set(pluginHolder.extension.android)
    loadedReporters.set(loadReportersTask.flatMap { it.loadedReporters })
    enableExperimentalRules.set(pluginHolder.extension.enableExperimentalRules)
    maxRuleVersion.set(pluginHolder.extension.maxRuleVersion)

    additionalTaskConfig()
}

internal fun createGenerateBaselineTask(
    project: Project,
    lintTasks: TaskCollection<out BaseKtLintCheckTask>
): TaskProvider<GenerateBaselineTask> {
    val ktlintExtension = project.extensions.getByType(KtlintExtension::class.java)
    val ktlintConfiguration = project.configurations.named(KTLINT_CONFIGURATION_NAME)
    return project.tasks.register(
        GenerateBaselineTask.NAME,
        GenerateBaselineTask::class.java
    ) {
        description = GenerateBaselineTask.DESCRIPTION
        group = HELP_GROUP

        dependsOn(lintTasks)

        ktLintClasspath.setFrom(ktlintConfiguration)
        baselineReporterClasspath.setFrom(project.configurations.named(KTLINT_BASELINE_REPORTER_CONFIGURATION_NAME))
        discoveredErrors.from(lintTasks.map { it.discoveredErrors })
        ktLintVersion.set(ktlintExtension.version)
        baselineFile.set(ktlintExtension.baseline)
        projectDirectory.set(project.layout.projectDirectory.asFile.absolutePath)
    }
}
