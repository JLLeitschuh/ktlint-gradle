package org.jlleitschuh.gradle.ktlint.worker


import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import java.io.File

class KtLintInvocation49(
    private val engine: KtLintRuleEngine
) : KtLintInvocation {
    companion object Factory : KtLintInvocationFactory {
        fun initialize(
            userData: Map<String, String>,
            enableExperimental: Boolean,
            disabledRules: Set<String>
        ): KtLintInvocation {

            val engine = KtLintRuleEngine(
                ruleProviders = StandardRuleSetProvider().getRuleProviders(),
//                editorConfigOverride = editorConfigOverride,
//                editorConfigDefaults = EditorConfigDefaults.load(
//                    path = Paths.get("/some/path/to/editorconfig/file/or/directory"),
//                    propertyTypes = KTLINT_API_CONSUMER_RULE_PROVIDERS.propertyTypes(),
//                )
            )
            return KtLintInvocation49(engine)
        }
    }

    override fun invokeLint(file: File): LintErrorResult {
        val errors = mutableListOf<Pair<SerializableLintError, Boolean>>()
        engine.lint(Code.fromFile(file)) { le: LintError ->
            errors.add(le.toSerializable() to false)
        }
        return LintErrorResult(file,errors)
    }

    override fun invokeFormat(file: File): Pair<String, LintErrorResult> {
        val errors = mutableListOf<Pair<SerializableLintError, Boolean>>()
        val newCode =
            engine.format(Code.fromFile(file)) { le, boolean ->
                errors.add(le.toSerializable() to boolean)
            }
        return newCode to LintErrorResult(file, errors)
    }
}

internal fun LintError.toSerializable(): SerializableLintError {
    return SerializableLintError(
        line, col, ruleId.value, detail, canBeAutoCorrected
    )
}
private fun Map<String, Set<RuleProvider>>.filterRules(
    enableExperimental: Boolean,
    disabledRules: Set<String>
): Set<RuleProvider> {
    return this.filterKeys { enableExperimental || it != "experimental" }
        .filterKeys { !(disabledRules.contains("standard") && it == "\u0000standard") }
        .toSortedMap()
        .mapValues { it.value }
        .values
        .flatten()
        .toSet()
}
