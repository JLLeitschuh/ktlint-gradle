package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.api.loadBaseline

class BaselineLoader48 : BaselineLoader {
    override fun loadBaselineRules(path: String): Map<String, List<SerializableLintError>> {
        return loadBaseline(path).lintErrorsPerFile.mapValues { it.value.map { it.toSerializable() } }
    }
}
