package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.LintError

interface BaselineReporterAdapter {
    fun onLintError(file: String, err: SerializableLintError, corrected: Boolean)
}

interface BaselineReporterAdapterFactory
