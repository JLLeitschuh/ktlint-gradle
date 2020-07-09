package org.jlleitschuh.gradle.ktlint

import java.io.PrintWriter
import javax.inject.Inject
import org.gradle.api.file.FileType
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges

@Suppress("UnstableApiUsage")
@CacheableTask
open class KtlintCheckTask @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout
) : BaseKtlintCheckTask(objectFactory, projectLayout) {
    override fun additionalConfig(): (PrintWriter) -> Unit = {}

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
        logger.debug("Files changed: $filesToLint")

        runLint(filesToLint)
    }
}
