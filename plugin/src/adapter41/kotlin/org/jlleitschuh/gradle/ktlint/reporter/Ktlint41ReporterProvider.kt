package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

class Ktlint41ReporterProvider(val reporterProvider: ReporterProvider) : GenericReporterProvider<Ktlint41Reporter> {
    companion object Factory : ReporterProviderFactory {
        fun initialize(reporterProvider: ReporterProvider): Ktlint41ReporterProvider =
            Ktlint41ReporterProvider(reporterProvider)
    }
    override fun get(out: PrintStream, opt: Map<String, String>): Ktlint41Reporter {
    return    Ktlint41Reporter(reporterProvider.get(out, opt))
    }

    override val id: String
        get() = reporterProvider.id
}
