package org.jlleitschuh.gradle.ktlint.sample

import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import java.io.PrintStream

class CsvReporter(private val out: PrintStream) : ReporterV2 {
    override fun onLintError(
        file: String,
        ktlintCliError: KtlintCliError,
    ) {
        out.println(
            "$file;${ktlintCliError.line};${ktlintCliError.col};${ktlintCliError.ruleId};${ktlintCliError.detail}",
        )
    }
}
