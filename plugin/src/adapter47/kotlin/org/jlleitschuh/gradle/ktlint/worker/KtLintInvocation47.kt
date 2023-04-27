package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.RuleSetProviderV2
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.ktlintDisabledRulesProperty
import com.pinterest.ktlint.core.api.EditorConfigOverride
import java.io.File
import java.util.*

class KtLintInvocation47(
    private val editorConfigPath: String?,
    private val ruleProviders: Set<RuleProvider>,
    private val userData: Map<String, String>,
    private val debug: Boolean
) : KtLintInvocation {
    companion object Factory : KtLintInvocationFactory {
        fun initialize(
            editorConfigPath: String?,
            userData: Map<String, String>,
            debug: Boolean,
            enableExperimental: Boolean,
            disabledRules: Set<String>
        ): KtLintInvocation =
            KtLintInvocation47(
                editorConfigPath,
                loadRuleSetsFromClasspathWithRuleSetProviderV2().filterRules(
                    enableExperimental,
                    disabledRules
                ),
                userData,
                debug
            )
    }

    private fun buildParams(
        file: File,
        cb: (LintError, Boolean) -> Unit
    ): KtLint.ExperimentalParams {
        val script = !file.name.endsWith(".kt", ignoreCase = true)
        return KtLint.ExperimentalParams(
            fileName = file.absolutePath,
            text = file.readText(),
            ruleProviders = ruleProviders,
            cb = cb,
            script = script,
            editorConfigPath = editorConfigPath,
            debug = debug,
            editorConfigOverride = userDataToEditorConfigOverride(userData)
        )
    }

    override fun invokeLint(file: File): LintErrorResult {
        val errors = mutableListOf<Pair<SerializableLintError, Boolean>>()
        KtLint.lint(buildParams(file) { le, boolean ->
            errors.add(le.toSerializable() to boolean)
        })
        return LintErrorResult(file, errors)
    }

    override fun invokeFormat(file: File): Pair<String, LintErrorResult> {
        val errors = mutableListOf<Pair<SerializableLintError, Boolean>>()
        val newCode = KtLint.format(buildParams(file) { le, boolean ->
            errors.add(le.toSerializable() to boolean)
        })
        return newCode to LintErrorResult(file, errors)
    }
}

internal fun LintError.toSerializable(): SerializableLintError {
    return SerializableLintError(
        line, col, ruleId, detail, canBeAutoCorrected
    )
}

private fun userDataToEditorConfigOverride(userData: Map<String, String>): EditorConfigOverride {
    val codeStyle = if (userData["android"]?.toBoolean() == true) {
        DefaultEditorConfigProperties.CodeStyleValue.android
    } else {
        DefaultEditorConfigProperties.CodeStyleValue.official
    }
    if (!userData["disabled_rules"].isNullOrBlank()) {
        return EditorConfigOverride.from(
            DefaultEditorConfigProperties.codeStyleSetProperty to codeStyle,
            ktlintDisabledRulesProperty to userData["disabled_rules"]
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
