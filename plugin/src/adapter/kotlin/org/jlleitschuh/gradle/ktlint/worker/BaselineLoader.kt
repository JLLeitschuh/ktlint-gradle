package org.jlleitschuh.gradle.ktlint.worker

interface BaselineLoader {
    fun loadBaselineRules(path: String): Map<String, List<SerializableLintError>>
}
