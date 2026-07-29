package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.EditorConfigPropertyRegistry
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import net.swiftzer.semver.SemVer
import java.io.File
import java.util.ServiceLoader

class KtLintInvocation100(
    private val engine: KtLintRuleEngine
) : KtLintInvocation {
    companion object Factory : KtLintInvocationFactory {
        fun initialize(
            editorConfigOverrides: Map<String, String>,
            enableExperimentalRules: Boolean,
            maxRuleVersion: String? = null
        ): KtLintInvocation {
            val ruleProviders = loadRuleSetsFromClasspathWithRuleSetProviderV3(enableExperimentalRules, maxRuleVersion)
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

        private fun loadRuleSetsFromClasspathWithRuleSetProviderV3(enableExperimentalRules: Boolean, maxRuleVersion: String?): Set<RuleProvider> {
            val allRuleProviders = ServiceLoader
                .load(RuleSetProviderV3::class.java)
                .flatMap { it.getRuleProviders() }
                .toSet()

            return if (maxRuleVersion != null || !enableExperimentalRules) {
                filterRules(allRuleProviders, enableExperimentalRules, maxRuleVersion)
            } else {
                allRuleProviders
            }
        }

        private fun filterRules(ruleProviders: Set<RuleProvider>, enableExperimentalRules: Boolean, maxVersion: String?): Set<RuleProvider> {
            return ruleProviders.filter { ruleProvider ->
                isRuleCompatibleWithFilters(ruleProvider, enableExperimentalRules, maxVersion)
            }.toSet()
        }

        private const val SINCE_KTLINT_CLASS = "com.pinterest.ktlint.rule.engine.core.api.SinceKtlint"
        private const val SINCE_KTLINT_STATUS_STABLE = "STABLE"

        // TODO: When bumping ktlint compile dependency to 1.8.0+, replace reflection with direct
        //  SinceKtlint import and use: ruleClass.getAnnotationsByType(SinceKtlint::class.java)

        /**
         * Determines if a rule should be included based on version and experimental filters.
         *
         * Uses reflection to access @SinceKtlint annotation, allowing this to work as a no-op
         * on older ktlint versions where the annotation doesn't exist.
         *
         * A rule is compatible if ANY of its @SinceKtlint annotations satisfy BOTH:
         * - Version constraint: rule version ≤ maxVersion (if maxVersion is set)
         * - Experimental constraint: enableExperimentalRules = true OR annotation status = STABLE
         *
         * Rules without @SinceKtlint annotations are always included for backwards compatibility.
         */
        internal fun isRuleCompatibleWithFilters(ruleProvider: RuleProvider, enableExperimentalRules: Boolean, maxVersion: String?): Boolean {
            val ruleClass = ruleProvider.createNewRuleInstance()::class.java

            val sinceKtlintClass = try {
                Class.forName(SINCE_KTLINT_CLASS)
            } catch (_: ClassNotFoundException) {
                // Annotation doesn't exist in this ktlint version
                return true
            }

            @Suppress("UNCHECKED_CAST")
            val sinceAnnotations = ruleClass.getAnnotationsByType(sinceKtlintClass as Class<Annotation>)
            if (sinceAnnotations.isEmpty()) {
                return true
            }

            return sinceAnnotations.any { annotation ->
                val version = sinceKtlintClass.getMethod("version").invoke(annotation).toString()
                val status = sinceKtlintClass.getMethod("status").invoke(annotation).toString()
                val versionCompatible = maxVersion == null || isVersionCompatible(version, maxVersion)
                val experimentalCompatible = enableExperimentalRules || status == SINCE_KTLINT_STATUS_STABLE
                versionCompatible && experimentalCompatible
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
