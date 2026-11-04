package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.GenerateBaselineTask
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

const val CHECK_KOTLIN_SCRIPT_TASK_NAME = "runKtlintCheckOverKotlinScripts"
const val FORMAT_KOTLIN_SCRIPT_TASK_NAME = "runKtlintFormatOverKotlinScripts"

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
    project: Project,
    sourceSetName: String,
    kotlinSourceDirectories: SourceDirectorySet
): TaskProvider<KtLintFormatTask> {
    val task = KtLintFormatTask.register(
        project,
        kotlinSourceDirectories.sourceDirectories,
        KtLintFormatTask.buildTaskNameForSourceSet(sourceSetName)
    )
    return task
}

internal fun createCheckTask(
    project: Project,
    sourceSetName: String,
    kotlinSourceDirectories: SourceDirectorySet
): TaskProvider<KtLintCheckTask> {
    val task = KtLintCheckTask.register(
        project,
        kotlinSourceDirectories.sourceDirectories,
        KtLintCheckTask.buildTaskNameForSourceSet(sourceSetName)
    )
    return task
}

internal fun createKotlinScriptCheckTask(project: Project): TaskProvider<KtLintCheckTask> {
    val projectScriptFiles = project.fileTree(project.projectDir)
    projectScriptFiles.include("*.kts")
    val task = KtLintCheckTask.register(project, projectScriptFiles, CHECK_KOTLIN_SCRIPT_TASK_NAME)
    return task
}

internal fun createKotlinScriptFormatTask(project: Project): TaskProvider<KtLintFormatTask> {
    val projectScriptFiles = project.fileTree(project.projectDir)
    projectScriptFiles.include("*.kts")
    val task = KtLintFormatTask.register(project, projectScriptFiles, FORMAT_KOTLIN_SCRIPT_TASK_NAME)
    return task
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
