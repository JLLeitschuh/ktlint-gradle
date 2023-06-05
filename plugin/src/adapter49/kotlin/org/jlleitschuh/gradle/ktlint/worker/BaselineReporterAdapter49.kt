package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2

class BaselineReporterAdapter49(val reporter: ReporterV2) : BaselineReporterAdapter {
    companion object Factory : BaselineReporterAdapterFactory {
        fun initialize(reporter: ReporterV2): BaselineReporterAdapter {
            return BaselineReporterAdapter49(reporter)
        }
    }

    override fun onLintError(file: String, err: SerializableLintError, corrected: Boolean) {
        reporter.onLintError(file, err.toCliError())
    }
}
