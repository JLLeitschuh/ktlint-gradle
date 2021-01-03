package org.jlleitschuh.gradle.ktlint

import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.jlleitschuh.gradle.ktlint.tasks.LoadReportersTask
import org.jlleitschuh.gradle.ktlint.tasks.LoadRuleSetsTask

internal fun KtlintPlugin.PluginHolder.addGenerateReportsTaskToProjectMetaCheckTask(
    generatesReportsTask: TaskProvider<GenerateReportsTask>
) {
    metaKtlintCheckTask.configure { it.dependsOn(generatesReportsTask) }
}

internal fun KtlintPlugin.PluginHolder.addGenerateReportsTaskToProjectMetaFormatTask(
    generateReportsTask: TaskProvider<GenerateReportsTask>
) {
    metaKtlintFormatTask.configure { it.dependsOn(generateReportsTask) }
}

internal fun createFormatTask(
    pluginHolder: KtlintPlugin.PluginHolder,
    sourceSetName: String,
    kotlinSourceDirectories: Iterable<*>
): TaskProvider<KtLintFormatTask> = pluginHolder
    .target
    .registerTask(
        KtLintFormatTask.buildTaskNameForSourceSet(sourceSetName)
    ) {
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
        KtLintCheckTask.buildTaskNameForSourceSet(sourceSetName)
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
    .registerTask(KtLintCheckTask.KOTLIN_SCRIPT_TASK_NAME) {
        description = KtLintCheckTask.buildDescription(".kts")
        configureBaseCheckTask(pluginHolder) {
            source = projectScriptFiles
        }
    }

internal fun createKotlinScriptFormatTask(
    pluginHolder: KtlintPlugin.PluginHolder,
    projectScriptFiles: FileTree
): TaskProvider<KtLintFormatTask> = pluginHolder
    .target
    .registerTask(KtLintFormatTask.KOTLIN_SCRIPT_TASK_NAME) {
        description = KtLintFormatTask.buildDescription(".kts")
        configureBaseCheckTask(pluginHolder) {
            source = projectScriptFiles
        }
    }

internal fun KtlintPlugin.PluginHolder.setCheckTaskDependsOnGenerateReportsTask(
    generateReportsTask: TaskProvider<GenerateReportsTask>
) {
    target.plugins.withType(LifecycleBasePlugin::class.java) {
        target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { task ->
            task.dependsOn(generateReportsTask)
        }
    }
}

internal fun createLoadRuleSetsTask(
    pluginHolder: KtlintPlugin.PluginHolder
): TaskProvider<LoadRuleSetsTask> = pluginHolder.target.registerTask(
    LoadRuleSetsTask.LOAD_RULE_SETS_TASK
) {
    description = LoadRuleSetsTask.DESCRIPTION

    ktLintClasspath.setFrom(pluginHolder.ktlintConfiguration)
    ruleSetsClasspath.setFrom(pluginHolder.ktlintReporterConfiguration)
    disabledRules.set(pluginHolder.extension.disabledRules)
    enableExperimentalRules.set(pluginHolder.extension.enableExperimentalRules)
    ktLintVersion.set(pluginHolder.extension.version)
}

internal fun createLoadReportersTask(
    pluginHolder: KtlintPlugin.PluginHolder
): TaskProvider<LoadReportersTask> = pluginHolder.target.registerTask(
    LoadReportersTask.TASK_NAME
) {
    description = LoadReportersTask.DESCRIPTION

    ktLintClasspath.setFrom(pluginHolder.ktlintConfiguration)
    reportersClasspath.setFrom(pluginHolder.ktlintReporterConfiguration)
    debug.set(pluginHolder.extension.debug)
    ktLintVersion.set(pluginHolder.extension.version)
    enabledReporters.set(pluginHolder.extension.reporterExtension.reporters)
    customReporters.set(pluginHolder.extension.reporterExtension.customReporters)
}

private fun BaseKtLintCheckTask.configureBaseCheckTask(
    pluginHolder: KtlintPlugin.PluginHolder,
    additionalTaskConfig: BaseKtLintCheckTask.() -> Unit
) {
    ktLintClasspath.setFrom(pluginHolder.ktlintConfiguration)
    ktlintVersion.set(pluginHolder.extension.version)
    additionalEditorconfigFile.set(pluginHolder.extension.additionalEditorconfigFile)
    debug.set(pluginHolder.extension.debug)
    ruleSetsClasspath.setFrom(pluginHolder.ktlintRulesetConfiguration)
    android.set(pluginHolder.extension.android)
    disabledRules.set(pluginHolder.extension.disabledRules)
    loadedRuleSets.set(pluginHolder.loadRuleSetsTask.get().loadedRuleSets)
    loadedReporters.set(pluginHolder.loadReportersTask.get().loadedReporters)

    additionalTaskConfig()
}

internal fun <T : BaseKtLintCheckTask> createGenerateReportsTask(
    pluginHolder: KtlintPlugin.PluginHolder,
    lintTask: TaskProvider<T>,
    lintType: GenerateReportsTask.LintType,
    sourceSetName: String
): TaskProvider<GenerateReportsTask> = pluginHolder.target.registerTask(
    GenerateReportsTask.generateNameForSourceSets(sourceSetName, lintType)
) {
    reportsName.set(GenerateReportsTask.generateNameForSourceSets(sourceSetName, lintType))
    commonConfiguration(pluginHolder, lintTask)
}

internal fun <T : BaseKtLintCheckTask> createKotlinScriptGenerateReportsTask(
    pluginHolder: KtlintPlugin.PluginHolder,
    lintTask: TaskProvider<T>,
    lintType: GenerateReportsTask.LintType
): TaskProvider<GenerateReportsTask> = pluginHolder.target.registerTask(
    GenerateReportsTask.generateNameForKotlinScripts(lintType)
) {
    reportsName.set(GenerateReportsTask.generateNameForKotlinScripts(lintType))
    commonConfiguration(pluginHolder, lintTask)
}

private fun <T : BaseKtLintCheckTask> GenerateReportsTask.commonConfiguration(
    pluginHolder: KtlintPlugin.PluginHolder,
    lintTask: TaskProvider<T>
) {
    description = GenerateReportsTask.DESCRIPTION
    dependsOn(lintTask)

    ktLintClasspath.setFrom(pluginHolder.ktlintConfiguration)
    reportersClasspath.setFrom(pluginHolder.ktlintReporterConfiguration)
    discoveredErrors.set(lintTask.get().discoveredErrors)
    loadedReporterProviders.set(pluginHolder.loadReportersTask.get().loadedReporterProviders)
    loadedReporters.set(pluginHolder.loadReportersTask.get().loadedReporters)
    enabledReporters.set(pluginHolder.extension.reporterExtension.reporters)
    outputToConsole.set(pluginHolder.extension.outputToConsole)
    coloredOutput.set(pluginHolder.extension.coloredOutput)
    outputColorName.set(pluginHolder.extension.outputColorName)
    ignoreFailures.set(pluginHolder.extension.ignoreFailures)
    verbose.set(pluginHolder.extension.verbose)
}
