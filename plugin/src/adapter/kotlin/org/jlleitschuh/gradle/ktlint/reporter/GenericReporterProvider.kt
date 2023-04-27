package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.core.Reporter
import java.io.PrintStream

/**
 * Abstraction over ReporterProvider and ReporterProviderV2
 */
interface GenericReporterProvider<T : GenericReporter<*>> {
    val id: String
    fun get(
        out: PrintStream,
        opt: Map<String, String>,
    ): T
}
