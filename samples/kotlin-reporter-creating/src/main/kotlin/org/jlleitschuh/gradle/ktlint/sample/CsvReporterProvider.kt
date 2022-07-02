package org.jlleitschuh.gradle.ktlint.sample

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

class CsvReporterProvider : ReporterProvider<Reporter> {
    override val id: String = "csv"

    override fun get(
        out: PrintStream,
        opt: Map<String, String>
    ): Reporter = CsvReporter(out)
}
