package org.jlleitschuh.gradle.ktlint.worker

interface BaselineReporterAdapter {
    fun onLintError(file: String, err: SerializableLintError, corrected: Boolean)
}

interface BaselineReporterAdapterFactory
