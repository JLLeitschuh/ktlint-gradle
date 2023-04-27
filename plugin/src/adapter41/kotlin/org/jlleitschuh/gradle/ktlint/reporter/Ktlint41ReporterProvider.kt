package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

class Ktlint41ReporterProvider(val reporterProvider: ReporterProvider) : GenericReporterProvider<Ktlint41Reporter> {
    override fun get(outputStream: PrintStream, opt: Map<String, String>): Ktlint41Reporter {
        return Ktlint41Reporter(reporterProvider.get(outputStream, opt))
    }

    override val id: String
        get() = reporterProvider.id
}
