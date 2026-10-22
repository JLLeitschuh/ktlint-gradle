package org.jlleitschuh.gradle.ktlint.reporter

import io.github.ktlint.core.cli.reporter.core.api.ReporterProviderV2
import java.io.PrintStream

class Ktlint2ReporterProviderV2ReporterProvider(
    val reporterProvider: ReporterProviderV2<*>
) : GenericReporterProvider<Ktlint2ReporterV2Reporter> {
    override fun get(outputStream: PrintStream, opt: Map<String, String>): Ktlint2ReporterV2Reporter {
        return Ktlint2ReporterV2Reporter(reporterProvider.get(outputStream, opt))
    }

    override val id: String
        get() = reporterProvider.id
}
