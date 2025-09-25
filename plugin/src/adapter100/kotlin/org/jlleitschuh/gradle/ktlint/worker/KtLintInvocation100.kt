package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.EditorConfigPropertyRegistry
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import net.swiftzer.semver.SemVer
import java.io.File
import java.util.ServiceLoader

class KtLintInvocation100(
    private val engine: KtLintRuleEngine
) : KtLintInvocation {
    companion object Factory : KtLintInvocationFactory {
        fun initialize(
            editorConfigOverrides: Map<String, String>,
            maxRuleVersion: String? = null
        ): KtLintInvocation {
            val ruleProviders = loadRuleSetsFromClasspathWithRuleSetProviderV3(maxRuleVersion)
            val editorConfigPropertyRegistry = EditorConfigPropertyRegistry(ruleProviders)
            val engine = if (editorConfigOverrides.isEmpty()) {
                KtLintRuleEngine(ruleProviders = ruleProviders)
            } else {
                KtLintRuleEngine(
                    ruleProviders = ruleProviders,
                    editorConfigOverride = EditorConfigOverride.from(
                        *editorConfigOverrides
                            .mapKeys { editorConfigPropertyRegistry.find(it.key) }
                            .entries
                            .map { it.key to it.value }
                            .toTypedArray()
                    )
                )
            }
            return KtLintInvocation100(engine)
        }

        private fun loadRuleSetsFromClasspathWithRuleSetProviderV3(maxRuleVersion: String?): Set<RuleProvider> {
            val allRuleProviders = ServiceLoader
                .load(RuleSetProviderV3::class.java)
                .flatMap { it.getRuleProviders() }
                .toSet()

            return if (maxRuleVersion != null) {
                filterRulesByVersion(allRuleProviders, maxRuleVersion)
            } else {
                allRuleProviders
            }
        }

        private fun filterRulesByVersion(ruleProviders: Set<RuleProvider>, maxVersion: String): Set<RuleProvider> {
            return ruleProviders.filter { ruleProvider ->
                isRuleCompatibleWithVersion(ruleProvider, maxVersion)
            }.toSet()
        }

        private fun isRuleCompatibleWithVersion(ruleProvider: RuleProvider, maxVersion: String): Boolean {
            // Use reflection to check for @SinceKtlint annotation
            val ruleClass = ruleProvider.createNewRuleInstance()::class.java
            val sinceAnnotation = ruleClass.getAnnotation(SinceKtlint::class.java)

            return if (sinceAnnotation != null) {
                isVersionCompatible(sinceAnnotation.version, maxVersion)
            } else {
                // If no annotation, assume it's from an older ktlint version (before annotations were included
                // at runtime) and include it for backward compatibility
                true
            }
        }

        /**
         * Checks if a rule version is compatible with the specified maximum version.
         *
         * A rule is considered compatible if its version is less than or equal to the maximum version.
         * Version comparison follows semantic versioning rules:
         * - "0.46" <= "1.0.0" → true
         * - "1.2.1" <= "1.2.0" → false
         * - "1.0" <= "1.0.0" → true (missing parts treated as 0)
         *
         * @param ruleVersion The version when the rule was introduced (from @SinceKtlint annotation)
         * @param maxVersion The maximum allowed version (from maxRuleVersion configuration)
         * @return true if the rule version is compatible (should be included), false otherwise
         */
        private fun isVersionCompatible(ruleVersion: String, maxVersion: String): Boolean {
            return try {
                SemVer.parse(ruleVersion) <= SemVer.parse(maxVersion)
            } catch (_: Exception) {
                // If version parsing fails (invalid format), assume it's incompatible.
                // We would only get to this point if the user is using a version with the runtime annotations
                // and they are specifically asking for a lower version.
                // All standard ktlint rules should have valid version formats.
                false
            }
        }
    }

    override fun invokeLint(file: File): LintErrorResult {
        val errors = mutableListOf<Pair<SerializableLintError, Boolean>>()
        engine.lint(Code.fromFile(file)) { le: LintError ->
            errors.add(le.toSerializable() to false)
        }
        return LintErrorResult(file, errors)
    }

    override fun invokeFormat(file: File): Pair<String, LintErrorResult> {
        val errors = mutableListOf<Pair<SerializableLintError, Boolean>>()
        val newCode =
            engine.format(Code.fromFile(file)) { le, boolean ->
                errors.add(le.toSerializable() to boolean)
            }
        return newCode to LintErrorResult(file, errors)
    }

    override fun trimMemory() {
        engine.trimMemory()
    }
}

internal fun LintError.toSerializable(): SerializableLintError {
    return SerializableLintError(line, col, ruleId.value, detail, canBeAutoCorrected)
}
