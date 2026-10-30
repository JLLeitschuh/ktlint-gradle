package org.jlleitschuh.gradle.ktlint.tasks

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.intermediateResultsBuildDir
import javax.inject.Inject

@CacheableTask
abstract class KtLintCheckTask @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    workerExecutor: WorkerExecutor
) : BaseKtLintCheckTask(workerExecutor, objectFactory, projectLayout) {

    @TaskAction
    fun lint(inputChanges: InputChanges) {
        runLint(inputChanges)
    }

    internal companion object {
        fun buildTaskNameForSourceSet(sourceSetName: String): String =
            "runKtlintCheckOver${sourceSetName.capitalize()}SourceSet"

        fun register(
            project: Project,
            sources: FileCollection,
            name: String
        ): TaskProvider<KtLintCheckTask> {
            return project.tasks.register(name, KtLintCheckTask::class.java) {
                configureBaseCheckTask(project)
                setSource(sources)
                discoveredErrors.set(project.layout.intermediateResultsBuildDir("${name}_errors.bin"))
                workerMaxHeapSize.convention("256m")
                description = "Lints all .kt files to ensure that they are formatted according to the code style."
            }
        }
    }
}
