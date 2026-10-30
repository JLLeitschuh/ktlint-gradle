package org.jlleitschuh.gradle.ktlint.reporter

import io.github.ktlint.core.cli.reporter.core.api.ReporterV2
import org.jlleitschuh.gradle.ktlint.worker.SerializableLintError
import org.jlleitschuh.gradle.ktlint.worker.toKtlint2CliError

class Ktlint2ReporterV2Reporter(val reporter: ReporterV2) : GenericReporter<ReporterV2> {
    override fun beforeAll() {
        reporter.beforeAll()
    }

    override fun before(file: String) {
        reporter.before(file)
    }

    override fun onLintError(file: String, err: SerializableLintError, corrected: Boolean) {
        reporter.onLintError(file, err.toKtlint2CliError())
    }

    override fun after(file: String) {
        reporter.after(file)
    }

    override fun afterAll() {
        reporter.afterAll()
    }
}
