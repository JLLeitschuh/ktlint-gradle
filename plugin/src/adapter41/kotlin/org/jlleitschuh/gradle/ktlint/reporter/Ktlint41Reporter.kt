package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Reporter
import org.jlleitschuh.gradle.ktlint.worker.SerializableLintError
import org.jlleitschuh.gradle.ktlint.worker.toCore

/**
 * implementation of GenericReporter for the Reporter ktlint class
 */
class Ktlint41Reporter(val reporter: Reporter) : GenericReporter<Reporter> {
    companion object Factory : ReporterFactory {
        fun initialize(reporter: Reporter): Ktlint41Reporter = Ktlint41Reporter(reporter)
    }
    override fun beforeAll() {
        reporter.beforeAll()
    }

    override fun before(file: String) {
        reporter.before(file)
    }

    override fun onLintError(file: String, err: SerializableLintError, corrected: Boolean) {
        reporter.onLintError(file, err.toCore(), corrected)
    }

    override fun after(file: String) {
        reporter.after(file)
    }

    override fun afterAll() {
        reporter.afterAll()
    }

}
