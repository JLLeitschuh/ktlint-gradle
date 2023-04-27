package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.LintError
import java.io.File
import java.io.Serializable

/**
 * Represents result of file code style check.
 * This handles files created with old versions of ktlint-gradle
 *
 * @param lintedFile file that was checked by KtLint
 * @param lintErrors list of found errors and flag indicating if this error was corrected
 */
data class LegacyLintErrorResult(
    val lintedFile: File,
    val lintErrors: List<Pair<LintError, Boolean>>
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 2012012585L
    }

    fun toNew(): LintErrorResult {
        return LintErrorResult(lintedFile, lintErrors.map {
            it.first.toSerializable() to it.second
        })
    }
}
