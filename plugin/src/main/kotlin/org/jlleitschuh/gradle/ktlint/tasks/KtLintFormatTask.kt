package org.jlleitschuh.gradle.ktlint.tasks

import org.gradle.api.file.FileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
abstract class KtLintFormatTask @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    workerExecutor: WorkerExecutor,
) : BaseKtLintCheckTask(
    objectFactory,
    projectLayout,
    workerExecutor,
) {

    @TaskAction
    fun format() {
        runLint(null, true)
    }

    /**
     * Fixes the issue when input file is restored to pre-format state and running format task again fails
     * with "up-to-date" task state.
     *
     * Note this approach sets task to "up-to-date" only on 3rd run when both input and output sources are the same.
     */
    @Suppress("unused")
    @OutputFiles
    fun getOutputSources(): FileTree {
        return source
    }

    internal companion object {
        fun buildTaskNameForSourceSet(
            sourceSetName: String
        ): String = "runKtlintFormatOver${sourceSetName.capitalize()}SourceSet"

        const val KOTLIN_SCRIPT_TASK_NAME = "runKtlintFormatOverKotlinScripts"

        fun buildDescription(
            fileType: String
        ): String = "Lints all $fileType files to ensure that they are formatted according to the code style " +
            " and, on error, tries to format code to conform code style."
    }
}
