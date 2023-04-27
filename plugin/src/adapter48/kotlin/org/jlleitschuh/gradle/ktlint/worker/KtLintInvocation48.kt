package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.KtLintRuleEngine
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleSetProviderV2
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.EditorConfigOverride
import com.pinterest.ktlint.core.api.editorconfig.CodeStyleValue
import java.io.File
import java.util.ServiceLoader

class KtLintInvocation48(
    private val engine: KtLintRuleEngine
) : KtLintInvocation {
    companion object Factory : KtLintInvocationFactory {
        fun initialize(
            userData: Map<String, String>,
            enableExperimental: Boolean,
            disabledRules: Set<String>
        ): KtLintInvocation {
            val editorConfigOverride = userDataToEditorConfigOverride(userData)
            val engine = KtLintRuleEngine(
                ruleProviders = loadRuleSetsFromClasspathWithRuleSetProviderV2().filterRules(
                    enableExperimental,
                    disabledRules
                ),
                editorConfigOverride = editorConfigOverride
            )
            return KtLintInvocation48(engine)
        }
    }

    override fun invokeLint(file: File): LintErrorResult {
        val errors = mutableListOf<Pair<SerializableLintError, Boolean>>()
        engine.lint(file.readText(), file.absoluteFile.toPath()) { le ->
            errors.add(le.toSerializable() to false)
        }
        return LintErrorResult(file, errors)
    }

    override fun invokeFormat(file: File): Pair<String, LintErrorResult> {
        val errors = mutableListOf<Pair<SerializableLintError, Boolean>>()
        val newCode =
            engine.format(file.absoluteFile.toPath()) { le, boolean ->
                errors.add(le.toSerializable() to boolean)
            }
        return newCode to LintErrorResult(file, errors)
    }

    override fun trimMemory() {
        engine.trimMemory()
    }
}

internal fun LintError.toSerializable(): SerializableLintError {
    return SerializableLintError(line, col, ruleId, detail, canBeAutoCorrected)
}

private fun userDataToEditorConfigOverride(userData: Map<String, String>): EditorConfigOverride {
    val codeStyle = if (userData["android"]?.toBoolean() == true) {
        CodeStyleValue.android
    } else {
        CodeStyleValue.official
    }
    if (!userData["disabled_rules"].isNullOrBlank()) {
        return EditorConfigOverride.from(
            DefaultEditorConfigProperties.codeStyleSetProperty to codeStyle,
            DefaultEditorConfigProperties.ktlintDisabledRulesProperty to userData["disabled_rules"]
        )
    }
    return EditorConfigOverride.from(DefaultEditorConfigProperties.codeStyleSetProperty to codeStyle)
}

private fun loadRuleSetsFromClasspathWithRuleSetProviderV2(): Map<String, Set<RuleProvider>> {
    return ServiceLoader
        .load(RuleSetProviderV2::class.java)
        .associateBy {
            val key = it.id
            // Adapted from KtLint CLI module
            if (key == "standard") "\u0000$key" else key
        }.mapValues {
            it.value.getRuleProviders()
        }
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
