package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import java.io.File

class KtLintInvocation45(
    private val editorConfigPath: String?,
    private val ruleSets: Set<com.pinterest.ktlint.core.RuleSet>,
    private val userData: Map<String, String>,
    private val debug: Boolean
) : KtLintInvocation {
    companion object Factory : KtLintInvocationFactory {
        fun initialize(
            editorConfigPath: String?,
            ruleSets: Set<com.pinterest.ktlint.core.RuleSet>,
            userData: Map<String, String>,
            debug: Boolean
        ): KtLintInvocation = KtLintInvocation45(
            editorConfigPath = editorConfigPath,
            ruleSets = ruleSets,
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
        val newCode = KtLint.format(buildParams(file) { le, boolean ->
            errors.add(le.toSerializable() to boolean)
        })
        return newCode to LintErrorResult(file, errors)
    }
}
