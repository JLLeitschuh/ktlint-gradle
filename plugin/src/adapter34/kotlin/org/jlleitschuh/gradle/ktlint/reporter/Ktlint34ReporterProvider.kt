package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

class Ktlint34ReporterProvider(val reporterProvider: ReporterProvider) : GenericReporterProvider<Ktlint34Reporter> {
    override fun get(outputStream: PrintStream, opt: Map<String, String>): Ktlint34Reporter {
        return Ktlint34Reporter(reporterProvider.get(outputStream, opt))
    }

    override val id: String
        get() = reporterProvider.id
}
