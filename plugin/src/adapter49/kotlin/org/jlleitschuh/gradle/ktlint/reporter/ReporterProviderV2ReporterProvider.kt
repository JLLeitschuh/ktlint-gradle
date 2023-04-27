package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.cli.reporter.core.api.ReporterProviderV2
import java.io.PrintStream

class ReporterProviderV2ReporterProvider(
    val reporterProvider: ReporterProviderV2<*>
) : GenericReporterProvider<ReporterV2Reporter> {
    override fun get(outputStream: PrintStream, opt: Map<String, String>): ReporterV2Reporter {
        return ReporterV2Reporter(reporterProvider.get(outputStream, opt))
    }

    override val id: String
        get() = reporterProvider.id
}
