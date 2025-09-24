package org.jlleitschuh.gradle.ktlint.worker

import java.io.File
import java.io.Serializable

/**
 * Represents result of file code style check.
 *
 * @param lintedFile file that was checked by KtLint
 * @param lintErrors list of found errors and flag indicating if this error was corrected
 */
data class LintErrorResult(
    val lintedFile: File,
    val lintErrors: List<Pair<SerializableLintError, Boolean>>
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 2012012585L
    }
}
