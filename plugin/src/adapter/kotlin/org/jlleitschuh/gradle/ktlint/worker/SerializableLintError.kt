package org.jlleitschuh.gradle.ktlint.worker

import java.io.Serializable

/**
 * Our Representation of a lint error to have consistent serialization and compatibility with multiple ktlint versions
 */
data class SerializableLintError(
    var line: Int = 0,
    var col: Int = 0,
    var ruleId: String = "",
    var detail: String = "",
    var canBeAutoCorrected: Boolean = false
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 20120922L
    }
}

fun List<SerializableLintError>.containsLintError(error: SerializableLintError): Boolean {
    return firstOrNull { lintError ->
        lintError.col == error.col &&
            lintError.line == error.line &&
            lintError.ruleId == error.ruleId
    } != null
}
