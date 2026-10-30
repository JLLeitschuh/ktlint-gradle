package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.GenerateBaselineTask
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.jlleitschuh.gradle.ktlint.tasks.LoadReportersTask

const val CHECK_KOTLIN_SCRIPT_TASK_NAME = "runKtlintCheckOverKotlinScripts"
const val FORMAT_KOTLIN_SCRIPT_TASK_NAME = "runKtlintFormatOverKotlinScripts"

internal fun addGenerateReportsTaskToProjectMetaCheckTask(
    project: Project, generatesReportsTask: TaskProvider<GenerateReportsTask>
) {
    project.tasks.named(CHECK_PARENT_TASK_NAME).configure { dependsOn(generatesReportsTask) }
}

internal fun addGenerateReportsTaskToProjectMetaFormatTask(
    project: Project, generateReportsTask: TaskProvider<GenerateReportsTask>
) {
    project.tasks.named(FORMAT_PARENT_TASK_NAME).configure { dependsOn(generateReportsTask) }
}

internal fun createFormatTask(
    project: Project, sourceSetName: String, kotlinSourceDirectories: SourceDirectorySet
): TaskProvider<KtLintFormatTask> {
    val task = KtLintFormatTask.register(
        project, kotlinSourceDirectories.sourceDirectories, KtLintFormatTask.buildTaskNameForSourceSet(sourceSetName)
    )
    return task
}

internal fun createCheckTask(
    project: Project, sourceSetName: String, kotlinSourceDirectories: SourceDirectorySet
): TaskProvider<KtLintCheckTask> {
    val task = KtLintCheckTask.register(
        project, kotlinSourceDirectories.sourceDirectories, KtLintCheckTask.buildTaskNameForSourceSet(sourceSetName)
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

internal fun setCheckTaskDependsOnGenerateReportsTask(
    project: Project,
    generateReportsTask: TaskProvider<GenerateReportsTask>
) {
    project.plugins.withType(LifecycleBasePlugin::class.java) {
        project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
            dependsOn(generateReportsTask)
        }
    }
}

fun <T : BaseKtLintCheckTask> GenerateReportsTask.commonConfiguration(
    project: Project, lintTask: TaskProvider<T>
) {
    val ktlintExtension = project.extensions.getByType(KtlintExtension::class.java)
    val ktlintConfiguration = project.configurations.named(KTLINT_CONFIGURATION_NAME)
    val ktlintReporterConfiguration = project.configurations.named(KTLINT_REPORTER_CONFIGURATION_NAME)
    val loadReportersTask = project.tasks.named(LoadReportersTask.TASK_NAME, LoadReportersTask::class.java)
    description = GenerateReportsTask.DESCRIPTION
    dependsOn(lintTask)

    ktLintClasspath.setFrom(ktlintConfiguration)
    reportersClasspath.setFrom(ktlintReporterConfiguration)
    discoveredErrors.set(lintTask.get().discoveredErrors)
    loadedReporterProviders.set(loadReportersTask.get().loadedReporterProviders)
    loadedReporters.set(loadReportersTask.get().loadedReporters)
    enabledReporters.set(ktlintExtension.reporterExtension.reporters)
    outputToConsole.set(ktlintExtension.outputToConsole)
    coloredOutput.set(ktlintExtension.coloredOutput)
    outputColorName.set(ktlintExtension.outputColorName)
    ignoreFailures.set(ktlintExtension.ignoreFailures)
    verbose.set(ktlintExtension.verbose)
    ktLintVersion.set(ktlintExtension.version)
    relative.set(ktlintExtension.relative)
    baseline.from(ktlintExtension.baseline)
}

internal fun createGenerateBaselineTask(
    project: Project, lintTasks: TaskCollection<out BaseKtLintCheckTask>
): TaskProvider<GenerateBaselineTask> {
    val ktlintExtension = project.extensions.getByType(KtlintExtension::class.java)
    val ktlintConfiguration = project.configurations.named(KTLINT_CONFIGURATION_NAME)
    return project.tasks.register(
        GenerateBaselineTask.NAME, GenerateBaselineTask::class.java
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
