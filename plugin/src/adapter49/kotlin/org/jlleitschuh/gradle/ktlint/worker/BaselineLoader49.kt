package org.jlleitschuh.gradle.ktlint.worker




class BaselineLoader49 : BaselineLoader {
    override fun loadBaselineRules(path: String): Map<String, List<SerializableLintError>> {
        return emptyMap()//loadBaseline(path).lintErrorsPerFile.mapValues { it.value.map { it.toSerializable() } }
    }
}
