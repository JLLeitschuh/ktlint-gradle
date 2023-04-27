package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.internal.loadBaseline


class BaselineLoader46 : BaselineLoader {
    override fun loadBaselineRules(path: String): Map<String, List<SerializableLintError>> {
        return loadBaseline(path).baselineRules?.mapValues { it.value.map { it.toSerializable() } } ?: emptyMap()
    }
}
