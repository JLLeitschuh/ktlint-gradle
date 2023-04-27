package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.Reporter

class BaselineReporterAdapter45(val reporter: Reporter) : BaselineReporterAdapter {
    companion object Factory: BaselineReporterAdapterFactory{
        fun initialize(reporter: Reporter): BaselineReporterAdapter{
            return BaselineReporterAdapter45(reporter)
        }
    }
    override fun onLintError(file: String, err: SerializableLintError, corrected: Boolean) {
        reporter.onLintError(file, err.toCore(), corrected)
    }
}
