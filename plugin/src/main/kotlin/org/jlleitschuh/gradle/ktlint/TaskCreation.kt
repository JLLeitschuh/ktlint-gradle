package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.util.PatternSet
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.GenerateBaselineTask
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
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
        mustRunAfter(project.tasks.named(KtLintFormatTask.KOTLIN_SCRIPT_TASK_NAME))

        val rootProjectName = project.rootProject.name
        var parentProject: Project? = project.parent
        while (parentProject != null && parentProject.name != rootProjectName) {
            val parentProjectPath = parentProject.path
            parentProject.plugins.withId("org.jlleitschuh.gradle.ktlint") {
                mustRunAfter("$parentProjectPath:${KtLintFormatTask.KOTLIN_SCRIPT_TASK_NAME}")
            }
            parentProject = parentProject.parent
        }

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
        KtLintFormatTask.KOTLIN_SCRIPT_TASK_NAME,
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

internal fun createLoadReportersTask(
    pluginHolder: KtlintPlugin.PluginHolder
): TaskProvider<LoadReportersTask> =
    pluginHolder.target.tasks.register<LoadReportersTask>(LoadReportersTask.TASK_NAME) {
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
    ktLintVersion.set(pluginHolder.extension.version)
    additionalEditorconfig.set(pluginHolder.extension.additionalEditorconfig)
    debug.set(pluginHolder.extension.debug)
    ruleSetsClasspath.setFrom(pluginHolder.ktlintRulesetConfiguration)
    android.set(pluginHolder.extension.android)
    disabledRules.set(pluginHolder.extension.disabledRules)
    loadedReporters.set(pluginHolder.loadReportersTask.get().loadedReporters)
    enableExperimentalRules.set(pluginHolder.extension.enableExperimentalRules)

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
    mustRunAfter(project.tasks.named(KtLintFormatTask.KOTLIN_SCRIPT_TASK_NAME))
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
    ktLintVersion.set(pluginHolder.extension.version)
    relative.set(pluginHolder.extension.relative)
    @Suppress("UnstableApiUsage")
    baseline.set(
        pluginHolder.extension.baseline
            .flatMap {
                if (it.asFile.exists()) {
                    pluginHolder.target.objects.fileProperty().apply { set(it.asFile) }
                } else {
                    pluginHolder.target.objects.fileProperty()
                }
            }
    )
}

internal fun createGenerateBaselineTask(
    pluginHolder: KtlintPlugin.PluginHolder,
    lintTasks: TaskCollection<out BaseKtLintCheckTask>
): TaskProvider<GenerateBaselineTask> = pluginHolder.target.registerTask(
    GenerateBaselineTask.NAME
) {
    description = GenerateBaselineTask.DESCRIPTION
    group = HELP_GROUP

    dependsOn(lintTasks)

    ktLintClasspath.setFrom(pluginHolder.ktlintConfiguration)
    baselineReporterClasspath.setFrom(pluginHolder.ktlintBaselineReporterConfiguration)
    discoveredErrors.from(lintTasks.map { it.discoveredErrors })
    ktLintVersion.set(pluginHolder.extension.version)
    baselineFile.set(pluginHolder.extension.baseline)
}
