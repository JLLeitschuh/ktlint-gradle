package org.jlleitschuh.gradle.ktlint.worker

import java.io.File
import java.io.Serializable

internal data class LintErrorResult(
    val lintedFile: File,
    val lintErrors: List<Pair<SerializableLintError, Boolean>>
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 2012012585L
    }
}
