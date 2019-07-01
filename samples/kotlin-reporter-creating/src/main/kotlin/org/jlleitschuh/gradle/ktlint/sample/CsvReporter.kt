package org.jlleitschuh.gradle.ktlint.sample

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import java.io.PrintStream

class CsvReporter(
    private val out: PrintStream
) : Reporter {
    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        out.println("$file;${err.line};${err.col};${err.ruleId};${err.detail};$corrected")
    }
}
