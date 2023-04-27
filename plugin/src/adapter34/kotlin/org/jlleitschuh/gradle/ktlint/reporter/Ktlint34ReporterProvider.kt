package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

class Ktlint34ReporterProvider(val reporterProvider: ReporterProvider) : GenericReporterProvider<Ktlint34Reporter> {
    companion object Factory : ReporterProviderFactory {
        fun initialize(reporterProvider: ReporterProvider): Ktlint34ReporterProvider =
            Ktlint34ReporterProvider(reporterProvider)
    }
    override fun get(out: PrintStream, opt: Map<String, String>): Ktlint34Reporter {
    return    Ktlint34Reporter(reporterProvider.get(out, opt))
    }

    override val id: String
        get() = reporterProvider.id
}
