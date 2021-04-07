package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.LintError
import java.io.File
import java.io.Serializable

/**
 * Represents result of file code style check.
 *
 * @param lintedFile file that was checked by KtLint
 * @param lintErrors list of found errors and flag indicating if this error was corrected
 */
internal data class LintErrorResult(
    val lintedFile: File,
    val lintErrors: List<Pair<LintError, Boolean>>
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 2012012585L
    }
}
