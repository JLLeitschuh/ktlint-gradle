package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.LintError
import java.lang.reflect.Method

/**
 * This file contains reflection code for accessing the Baseline API which had a breaking change in 0.46 and again in 0.47
 * This function is the entry point, which will load the lint errors from the baseline report in a version-neutral way
 */
internal fun loadBaselineRules(path: String): Map<String, List<LintError>> {
    return newBaselineClass?.let { loadNewBaselineRules(path) }
        ?: baselineClass46?.let { load46BaselineRules(path) }
        ?: loadOldBaselineRules(path)
}

private val newBaselineClass: Class<*>? by lazy {
    try {
        Class.forName("com.pinterest.ktlint.core.api.BaselineKt")
    } catch (e: Exception) {
        null
    }
}
private val oldBaselineClass: Class<*>? by lazy {
    try {
        Class.forName("com.pinterest.ktlint.core.internal.BaselineSupportKt")
    } catch (e: Exception) {
        null
    }
}
private val baselineClass46: Class<*>? by lazy {
    try {
        Class.forName("com.pinterest.ktlint.core.internal.CurrentBaselineKt")
    } catch (e: Exception) {
        null
    }
}

private val oldLoadMethod: Method? by lazy {
    oldBaselineClass?.declaredMethods?.firstOrNull { it.name == "loadBaseline" }
}

private val newLoadMethod: Method? by lazy {
    newBaselineClass?.declaredMethods?.firstOrNull { it.name == "loadBaseline" }
}

private val loadMethod46: Method? by lazy {
    baselineClass46?.declaredMethods?.firstOrNull { it.name == "loadBaseline" }
}

private fun load46BaselineRules(path: String): Map<String, List<LintError>> {
    val baseline = loadMethod46!!.invoke(null, path)
    return baseline.javaClass.getDeclaredMethod("getBaselineRules")
        .invoke(baseline) as Map<String, List<LintError>>
}

private fun loadOldBaselineRules(path: String): Map<String, List<LintError>> {
    val baseline = oldLoadMethod!!.invoke(null, path)
    return baseline.javaClass.getDeclaredMethod("getBaselineRules")
        .invoke(baseline) as Map<String, List<LintError>>
}

private fun loadNewBaselineRules(path: String): Map<String, List<LintError>> {
    val newBaseline = newLoadMethod!!.invoke(null, path)
    return newBaseline.javaClass.getDeclaredMethod("getLintErrorsPerFile")
        .invoke(newBaseline) as Map<String, List<LintError>>
}

/**
 * This helper method is not consistent across multiple ktlint versions, so it is copied here
 */
internal fun List<LintError>.containsLintError(error: LintError): Boolean {
    return firstOrNull { lintError ->
        lintError.col == error.col &&
            lintError.line == error.line &&
            lintError.ruleId == error.ruleId
    } != null
}
