package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties
import com.pinterest.ktlint.core.api.EditorConfigOverride
import java.io.File
import java.util.ServiceLoader

class KtLintInvocation46(
    private val editorConfigPath: String?,
    private val ruleSets: Set<RuleSet>,
    private val userData: Map<String, String>,
    private val debug: Boolean
) : KtLintInvocation {
    companion object Factory : KtLintInvocationFactory {
        fun initialize(
            editorConfigPath: String?,
            enableExperimental: Boolean,
            disabledRules: Set<String>,
            userData: Map<String, String>,
            debug: Boolean
        ): KtLintInvocation = KtLintInvocation46(
            editorConfigPath = editorConfigPath,
            ruleSets = loadRuleSetsFromClasspathWithRuleSetProvider()
                .filterRules(enableExperimental, disabledRules),
            userData = userData,
            debug = debug
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
            ruleSets = ruleSets,
            cb = cb,
            script = script,
            editorConfigPath = editorConfigPath,
            debug = debug,
            editorConfigOverride = userDataToEditorConfigOverride(userData)
        )
    }

    override fun invokeLint(file: File): LintErrorResult {
        val errors = mutableListOf<Pair<SerializableLintError, Boolean>>()
        KtLint.lint(
            buildParams(file) { le, boolean ->
                errors.add(le.toSerializable() to boolean)
            }
        )
        return LintErrorResult(file, errors)
    }

    override fun invokeFormat(file: File): Pair<String, LintErrorResult> {
        val errors = mutableListOf<Pair<SerializableLintError, Boolean>>()
        val newCode = KtLint.format(
            buildParams(file) { le, boolean ->
                errors.add(le.toSerializable() to boolean)
            }
        )
        return newCode to LintErrorResult(file, errors)
    }

    override fun trimMemory() {
        KtLint.trimMemory()
    }
}

internal fun LintError.toSerializable(): SerializableLintError {
    return SerializableLintError(line, col, ruleId, detail, canBeAutoCorrected)
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
            DefaultEditorConfigProperties.disabledRulesProperty to userData["disabled_rules"]
        )
    }
    return EditorConfigOverride.from(DefaultEditorConfigProperties.codeStyleSetProperty to codeStyle)
}

private fun loadRuleSetsFromClasspathWithRuleSetProvider(): Map<String, com.pinterest.ktlint.core.RuleSet> {
    return ServiceLoader
        .load(RuleSetProvider::class.java)
        .associateBy {
            val key = it.get().id
            // Adapted from KtLint CLI module
            if (key == "standard") "\u0000$key" else key
        }
        .mapValues { it.value.get() }
}
