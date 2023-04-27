package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.cli.reporter.core.api.ReporterProviderV2
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import java.io.PrintStream

class ReporterProviderV2ReporterProvider(
    val reporterProvider: ReporterProviderV2<ReporterV2>
) : GenericReporterProvider<ReporterV2Reporter> {
    companion object Factory : ReporterProviderFactory {
        fun initialize(reporterProvider: ReporterProviderV2<ReporterV2>): ReporterProviderV2ReporterProvider =
            ReporterProviderV2ReporterProvider(reporterProvider)
    }


    override fun get(out: PrintStream, opt: Map<String, String>): ReporterV2Reporter {
        return ReporterV2Reporter(reporterProvider.get(out, opt))
    }

    override val id: String
        get() = reporterProvider.id
}
