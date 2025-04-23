package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError

fun KtlintCliError.toSerializable(): SerializableLintError {
    return SerializableLintError(line, col, ruleId, detail, status.toBoolean())
}

fun KtlintCliError.Status.toBoolean(): Boolean {
    return when (this) {
        KtlintCliError.Status.LINT_CAN_NOT_BE_AUTOCORRECTED -> false
        KtlintCliError.Status.LINT_CAN_BE_AUTOCORRECTED -> true
        else -> false
    }
}

fun SerializableLintError.toCliError(): KtlintCliError {
    return KtlintCliError(
        line,
        col,
        ruleId,
        detail,
        if (canBeAutoCorrected) {
            KtlintCliError.Status.LINT_CAN_BE_AUTOCORRECTED
        } else {
            KtlintCliError.Status.LINT_CAN_NOT_BE_AUTOCORRECTED
        }
    )
}
