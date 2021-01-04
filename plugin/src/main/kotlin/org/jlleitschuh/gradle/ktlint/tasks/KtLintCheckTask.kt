package org.jlleitschuh.gradle.ktlint.tasks

import org.gradle.api.file.FileType
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
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
        logger.info("Executing ${if (inputChanges.isIncremental) "incrementally" else "non-incrementally"}")

        val filesToLint = inputChanges
            .getFileChanges(stableSources)
            .asSequence()
            .filter {
                it.fileType != FileType.DIRECTORY &&
                    it.changeType != ChangeType.REMOVED
            }
            .map { it.file }
            .toSet()

        if (filesToLint.isEmpty()) {
            didWork = false
            logger.info("No ${ChangeType.ADDED} or ${ChangeType.MODIFIED} files that need to be linted")
        } else {
            logger.debug("Files changed: $filesToLint")

            runLint(filesToLint, false)
        }
    }

    companion object {
        fun buildTaskNameForSourceSet(
            sourceSetName: String
        ): String = "runKtlintCheckOver${sourceSetName.capitalize()}SourceSet"

        const val KOTLIN_SCRIPT_TASK_NAME = "runKtlintCheckOverKotlinScripts"

        fun buildDescription(
            fileType: String
        ): String = "Lints all $fileType files to ensure that they are formatted according to the code style."
    }
}
