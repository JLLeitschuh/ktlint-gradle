package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import java.io.PrintStream

class LegacyKtlintReporterProvider(
    val reporterProvider: ReporterProvider<out Reporter>
) : GenericReporterProvider<LegacyKtlintReporter> {
    override fun get(outputStream: PrintStream, opt: Map<String, String>): LegacyKtlintReporter {
        return LegacyKtlintReporter(reporterProvider.get(outputStream, opt))
    }

    override val id: String
        get() = reporterProvider.id
}
