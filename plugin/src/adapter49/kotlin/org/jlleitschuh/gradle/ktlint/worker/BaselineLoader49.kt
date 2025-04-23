package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline

class BaselineLoader49 : BaselineLoader {
    override fun loadBaselineRules(path: String): Map<String, List<SerializableLintError>> {
        return loadBaseline(path).lintErrorsPerFile.mapValues { it.value.map { it.toSerializable() } }
    }
}
