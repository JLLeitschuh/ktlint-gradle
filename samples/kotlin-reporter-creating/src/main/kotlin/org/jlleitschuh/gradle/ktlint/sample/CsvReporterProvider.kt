package org.jlleitschuh.gradle.ktlint.sample

import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

class CsvReporterProvider : ReporterProvider<CsvReporter> {
    override val id: String = "csv"

    override fun get(
        out: PrintStream,
        opt: Map<String, String>
    ): CsvReporter = CsvReporter(out)
}
