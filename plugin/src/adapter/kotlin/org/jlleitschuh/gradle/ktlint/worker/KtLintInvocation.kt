package org.jlleitschuh.gradle.ktlint.worker

import java.io.File

/**
 * An abstraction for invoking ktlint across all breaking changes between versions
 */
interface KtLintInvocation {
    fun invokeLint(file: File): LintErrorResult
    fun invokeFormat(file: File): Pair<String, LintErrorResult>

    fun trimMemory()
}
