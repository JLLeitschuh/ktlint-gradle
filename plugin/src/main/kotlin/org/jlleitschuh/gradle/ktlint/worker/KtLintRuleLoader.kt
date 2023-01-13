package org.jlleitschuh.gradle.ktlint.worker

import java.util.ServiceLoader
import kotlin.reflect.full.memberProperties

/**
 * Old API for loading rules available prior to ktlint 0.47.0
 */
internal fun loadRuleSetsFromClasspathWithRuleSetProvider(): Map<String, com.pinterest.ktlint.core.RuleSet> {
    return ServiceLoader
        .load(com.pinterest.ktlint.core.RuleSetProvider::class.java)
        .associateBy {
            val key = it.get().id
            // Adapted from KtLint CLI module
            if (key == "standard") "\u0000$key" else key
        }
        .mapValues { it.value.get() }
}

/**
 * New API for loading rules available in ktlint 0.47+
 */
internal fun loadRuleSetsFromClasspathWithRuleSetProviderV2(): Map<String, Set<Any>> {
    val ruleSetProviderV2Class = Class.forName("com.pinterest.ktlint.core.RuleSetProviderV2")
    val idProperty = ruleSetProviderV2Class.kotlin.memberProperties.first { it.name == "id" }
    val getRuleProviders = ruleSetProviderV2Class.getDeclaredMethod("getRuleProviders")
    return ServiceLoader
        .load(ruleSetProviderV2Class)
        .associateBy {
            val key = idProperty.getter.call(it) as String
            // Adapted from KtLint CLI module
            if (key == "standard") "\u0000$key" else key
        }.mapValues {
            getRuleProviders.invoke(it.value) as Set<Any>
        }
}
