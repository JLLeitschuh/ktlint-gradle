package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.LintError

fun LintError.toSerializable(): SerializableLintError {
    return SerializableLintError(
        line, col, ruleId, detail, canBeAutoCorrected
    )
}

fun SerializableLintError.toCore(): LintError {
    return LintError(
        line, col, ruleId, detail, canBeAutoCorrected
    )
}
