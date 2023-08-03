package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import java.io.File
import java.util.ServiceLoader

class KtLintInvocation49(
    private val engine: KtLintRuleEngine
) : KtLintInvocation {
    companion object Factory : KtLintInvocationFactory {
        fun initialize(): KtLintInvocation {
            val engine = KtLintRuleEngine(
                ruleProviders = loadRuleSetsFromClasspathWithRuleSetProviderV3()
            )
            return KtLintInvocation49(engine)
        }

        private fun loadRuleSetsFromClasspathWithRuleSetProviderV3(): Set<RuleProvider> {
            return ServiceLoader
                .load(RuleSetProviderV3::class.java)
                .flatMap { it.getRuleProviders() }
                .toSet()
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
