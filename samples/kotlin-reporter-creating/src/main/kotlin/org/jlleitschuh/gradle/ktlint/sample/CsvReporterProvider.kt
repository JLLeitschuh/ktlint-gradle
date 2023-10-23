package org.jlleitschuh.gradle.ktlint.sample

import com.pinterest.ktlint.cli.reporter.core.api.ReporterProviderV2
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import java.io.PrintStream

class CsvReporterProvider : ReporterProviderV2<ReporterV2> {
    override val id: String = "csv"

    override fun get(
        out: PrintStream,
        opt: Map<String, String>,
    ): ReporterV2 = CsvReporter(out)
}
