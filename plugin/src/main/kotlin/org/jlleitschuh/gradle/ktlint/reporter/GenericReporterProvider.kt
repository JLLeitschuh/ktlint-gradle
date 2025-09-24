package org.jlleitschuh.gradle.ktlint.reporter

import java.io.PrintStream

/**
 * Abstraction over ReporterProvider and ReporterProviderV2
 */
interface GenericReporterProvider<T : GenericReporter<*>> {
    val id: String
    fun get(
        outputStream: PrintStream,
        opt: Map<String, String>
    ): T
}
