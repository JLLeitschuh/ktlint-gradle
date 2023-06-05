package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import java.io.File
import java.util.ServiceLoader

class KtLintInvocation45(
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
        ): KtLintInvocation = KtLintInvocation45(
            editorConfigPath = editorConfigPath,
            ruleSets = loadRuleSetsFromClasspathWithRuleSetProvider()
                .filterRules(enableExperimental, disabledRules),
            userData = userData,
            debug = debug
        )
    }

    private fun buildParams(file: File, cb: (LintError, Boolean) -> Unit): KtLint.Params {
        val script = !file.name.endsWith(".kt", ignoreCase = true)
        return KtLint.Params(
            fileName = file.absolutePath,
            text = file.readText(),
            ruleSets = ruleSets,
            userData = userData,
            debug = debug,
            editorConfigPath = editorConfigPath,
            script = script,
            cb = cb
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

private fun loadRuleSetsFromClasspathWithRuleSetProvider(): Map<String, RuleSet> {
    return ServiceLoader
        .load(RuleSetProvider::class.java)
        .associateBy {
            val key = it.get().id
            // Adapted from KtLint CLI module
            if (key == "standard") "\u0000$key" else key
        }
        .mapValues { it.value.get() }
}
