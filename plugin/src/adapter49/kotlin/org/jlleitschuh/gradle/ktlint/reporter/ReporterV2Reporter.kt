package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import org.jlleitschuh.gradle.ktlint.worker.SerializableLintError
import org.jlleitschuh.gradle.ktlint.worker.toCliError

class ReporterV2Reporter(val reporter: ReporterV2) : GenericReporter<ReporterV2> {
    override fun beforeAll() {
        reporter.beforeAll()
    }

    override fun before(file: String) {
        reporter.before(file)
    }

    override fun onLintError(file: String, err: SerializableLintError, corrected: Boolean) {
        reporter.onLintError(file, err.toCliError())
    }

    override fun after(file: String) {
        reporter.after(file)
    }

    override fun afterAll() {
        reporter.afterAll()
    }
}
