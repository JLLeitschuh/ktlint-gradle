package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.LintError

interface BaselineLoader {
    fun loadBaselineRules(path: String): Map<String, List<SerializableLintError>>
}
