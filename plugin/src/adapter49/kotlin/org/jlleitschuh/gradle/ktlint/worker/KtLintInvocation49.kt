package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import java.io.File
import java.util.ServiceLoader

class KtLintInvocation49(
    private val engine: KtLintRuleEngine
) : KtLintInvocation {
    companion object Factory : KtLintInvocationFactory {
        fun initialize(editorConfigOverrides: Map<String, String>): KtLintInvocation {
            val ruleProviders = loadRuleSetsFromClasspathWithRuleSetProviderV3()
            val engine = if (editorConfigOverrides.isEmpty()) {
                KtLintRuleEngine(ruleProviders = ruleProviders)
            } else {
                KtLintRuleEngine(
                    ruleProviders = ruleProviders,
                    editorConfigOverride = EditorConfigOverride.from(
                        *editorConfigOverrides
                            .mapKeys { ruleProviders.findEditorConfigProperty(it.key) }
                            .entries
                            .map { it.key to it.value }
                            .toTypedArray()
                    )
                )
            }
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

private fun Set<RuleProvider>.findEditorConfigProperty(propertyName: String): EditorConfigProperty<*> {
    val properties =
        map { it.createNewRuleInstance() }
            .flatMap { it.usesEditorConfigProperties }
            .distinct()
    return properties
        .find { it.type.name == propertyName }
        ?: throw RuntimeException(
            properties
                .map { it.type.name }
                .sorted()
                .joinToString(
                    prefix = "Property with name '$propertyName' is not found in any of given rules. Available properties:\n\t",
                    separator = "\n\t"
                ) { "- $it" }
        )
}
