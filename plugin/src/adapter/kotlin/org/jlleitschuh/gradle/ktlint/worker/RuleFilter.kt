package org.jlleitschuh.gradle.ktlint.worker

/**
 * Apply filter logic in a generic way that works with old and new rule loading APIs
 */
fun <T> Map<String, T>.filterRules(enableExperimental: Boolean, disabledRules: Set<String>): Set<T> {
    return this.filterKeys { enableExperimental || it != "experimental" }
        .filterKeys { !(disabledRules.contains("standard") && it == "\u0000standard") }
        .toSortedMap().mapValues { it.value }.values.toSet()
}
