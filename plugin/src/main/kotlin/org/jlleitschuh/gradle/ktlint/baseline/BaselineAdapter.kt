package org.jlleitschuh.gradle.ktlint.baseline

import org.jlleitschuh.gradle.ktlint.worker.LintErrorResult
import org.jlleitschuh.gradle.ktlint.worker.SerializableLintError
import java.io.File

interface BaselineAdapter<E> {
    fun loadBaselineFile(baselineFile: File)

    fun filterOutBaseline(
        projectDir: File,
        lintErrorResult: LintErrorResult
    ): List<Pair<SerializableLintError, Boolean>>
}
