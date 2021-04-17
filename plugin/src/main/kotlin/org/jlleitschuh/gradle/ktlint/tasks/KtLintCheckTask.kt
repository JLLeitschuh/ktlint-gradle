package org.jlleitschuh.gradle.ktlint.tasks

import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@Suppress("UnstableApiUsage")
@CacheableTask
abstract class KtLintCheckTask @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    workerExecutor: WorkerExecutor,
) : BaseKtLintCheckTask(
    objectFactory,
    projectLayout,
    workerExecutor,
) {

    @TaskAction
    fun lint(inputChanges: InputChanges) {
        runLint(inputChanges, false)
    }

    internal companion object {
        fun buildTaskNameForSourceSet(
            sourceSetName: String
        ): String = "runKtlintCheckOver${sourceSetName.capitalize()}SourceSet"

        const val KOTLIN_SCRIPT_TASK_NAME = "runKtlintCheckOverKotlinScripts"

        fun buildDescription(
            fileType: String
        ): String = "Lints all $fileType files to ensure that they are formatted according to the code style."
    }
}
