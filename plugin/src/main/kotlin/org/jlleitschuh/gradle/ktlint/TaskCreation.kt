package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin

internal fun KtlintPlugin.PluginHolder.addKtlintCheckTaskToProjectMetaCheckTask(
    checkTask: TaskProvider<KtlintCheckTask>
) {
    metaKtlintCheckTask.configure { it.dependsOn(checkTask) }
}

internal fun KtlintPlugin.PluginHolder.addKtlintFormatTaskToProjectMetaFormatTask(
    formatTask: TaskProvider<KtlintFormatTask>
) {
    metaKtlintFormatTask.configure { it.dependsOn(formatTask) }
}

internal fun createFormatTask(
    pluginHolder: KtlintPlugin.PluginHolder,
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

internal fun createCheckTask(
    pluginHolder: KtlintPlugin.PluginHolder,
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

internal fun createKotlinScriptCheckTask(
    pluginHolder: KtlintPlugin.PluginHolder,
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

internal fun createKotlinScriptFormatTask(
    pluginHolder: KtlintPlugin.PluginHolder,
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

internal fun setCheckTaskDependsOnKtlintCheckTask(
    project: Project,
    ktlintCheck: TaskProvider<KtlintCheckTask>
) {
    project.plugins.withType(LifecycleBasePlugin::class.java) {
        project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { task ->
            task.dependsOn(ktlintCheck)
        }
    }
}

private fun BaseKtlintCheckTask.configurePluginTask(
    pluginHolder: KtlintPlugin.PluginHolder,
    additionalTaskConfig: BaseKtlintCheckTask.() -> Unit
) {
    classpath.setFrom(pluginHolder.ktlintConfiguration)
    ktlintVersion.set(pluginHolder.extension.version)
    verbose.set(pluginHolder.extension.verbose)
    additionalEditorconfigFile.set(pluginHolder.extension.additionalEditorconfigFile)
    debug.set(pluginHolder.extension.debug)
    ignoreFailures.set(pluginHolder.extension.ignoreFailures)
    outputToConsole.set(pluginHolder.extension.outputToConsole)
    coloredOutput.set(
        pluginHolder.extension.coloredOutput.map {
            if (pluginHolder.target.isConsolePlain()) {
                pluginHolder.target.logger.info("Console type is plain: disabling colored output")
                false
            } else {
                it
            }
        }
    )
    outputColorName.set(pluginHolder.extension.outputColorName)
    ruleSetsClasspath.setFrom(pluginHolder.ktlintRulesetConfiguration)
    reporters.set(pluginHolder.extension.reporterExtension.reporters)
    customReportersClasspath.setFrom(pluginHolder.ktlintReporterConfiguration)
    customReporters.set(pluginHolder.extension.reporterExtension.customReporters)
    android.set(pluginHolder.extension.android)
    enableExperimentalRules.set(pluginHolder.extension.enableExperimentalRules)
    disabledRules.set(pluginHolder.extension.disabledRules)

    additionalTaskConfig()
}
