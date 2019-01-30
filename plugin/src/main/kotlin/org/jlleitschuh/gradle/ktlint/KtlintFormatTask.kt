package org.jlleitschuh.gradle.ktlint

import org.gradle.api.file.FileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.process.JavaExecSpec
import javax.inject.Inject

@CacheableTask
open class KtlintFormatTask @Inject constructor(
    objectFactory: ObjectFactory
) : KtlintCheckTask(objectFactory) {
    override fun additionalConfig(): (JavaExecSpec) -> Unit = {
        it.args("-F")
    }

    /**
     * Fixes the issue when input file is restored to pre-format state and running format task again fails
     * with "up-to-date" task state.
     *
     * Note this approach sets task to "up-to-date" only on 3rd run when both input and output sources are the same.
     */
    @OutputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getOutputSources(): FileTree { return getSource() }
}
