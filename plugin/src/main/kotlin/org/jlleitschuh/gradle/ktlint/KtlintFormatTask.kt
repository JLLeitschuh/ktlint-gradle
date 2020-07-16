package org.jlleitschuh.gradle.ktlint

import java.io.PrintWriter
import javax.inject.Inject
import org.gradle.api.file.FileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

@CacheableTask
open class KtlintFormatTask @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout
) : BaseKtlintCheckTask(objectFactory, projectLayout) {
    override fun additionalConfig(): (PrintWriter) -> Unit = {
        it.println("-F")
    }

    @TaskAction
    fun format() {
        runLint(stableSources.files)
    }

    /**
     * Fixes the issue when input file is restored to pre-format state and running format task again fails
     * with "up-to-date" task state.
     *
     * Note this approach sets task to "up-to-date" only on 3rd run when both input and output sources are the same.
     */
    @OutputFiles
    fun getOutputSources(): FileTree { return getSource() }
}
