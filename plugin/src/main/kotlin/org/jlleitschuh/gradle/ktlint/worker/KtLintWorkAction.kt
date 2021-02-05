package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.util.ServiceLoader

@Suppress("UnstableApiUsage")
abstract class KtLintWorkAction : WorkAction<KtLintWorkAction.KtLintWorkParameters> {
    override fun execute() {
        val ruleSets = loadRuleSetsAndFilterThem(
            parameters.enableExperimental.getOrElse(false),
            parameters.disabledRules.getOrElse(emptySet())
        )

        val additionalEditorConfig = parameters
            .additionalEditorconfigFile
            .orNull
            ?.asFile
            ?.absolutePath
        val userData = generateUserData()
        val debug = parameters.debug.get()
        val formatSource = parameters.formatSource.getOrElse(false)

        val result = mutableListOf<LintErrorResult>()

        parameters.filesToLint.files.forEach {
            val errors = mutableListOf<Pair<SerializableLintError, Boolean>>()
            val ktLintParameters = KtLint.Params(
                fileName = it.absolutePath,
                text = it.readText(),
                ruleSets = ruleSets,
                userData = userData,
                debug = debug,
                editorConfigPath = additionalEditorConfig,
                script = !it.name.endsWith(".kt", ignoreCase = true),
                cb = { lintError, isCorrected ->
                    errors.add(SerializableLintError(lintError) to isCorrected)
                }
            )

            if (formatSource) {
                val currentFileContent = it.readText()
                val updatedFileContent = KtLint.format(ktLintParameters)

                if (updatedFileContent != currentFileContent) {
                    it.writeText(updatedFileContent)
                }
            } else {
                KtLint.lint(ktLintParameters)
            }
            result.add(
                LintErrorResult(
                    lintedFile = it,
                    lintErrors = errors
                )
            )
        }

        ObjectOutputStream(
            BufferedOutputStream(
                FileOutputStream(
                    parameters.discoveredErrorsFile.asFile.get()
                )
            )
        ).use {
            it.writeObject(result)
        }
    }

    private fun generateUserData(): Map<String, String> {
        val userData = mutableMapOf(
            "android" to parameters.android.get().toString()
        )
        val disabledRules = parameters.disabledRules.get()
        if (disabledRules.isNotEmpty()) {
            userData["disabled_rules"] = disabledRules.joinToString(separator = ",")
        }

        return userData.toMap()
    }

    private fun loadRuleSetsAndFilterThem(
        enableExperimental: Boolean,
        disabledRules: Set<String>
    ): Set<RuleSet> = loadRuleSetsFromClasspath()
        .filterKeys { enableExperimental || it != "experimental" }
        .filterKeys { !(disabledRules.contains("standard") && it == "\u0000standard") }
        .toSortedMap()
        .mapValues { it.value.get() }
        .values
        .toSet()

    private fun loadRuleSetsFromClasspath(): Map<String, RuleSetProvider> = ServiceLoader
        .load(RuleSetProvider::class.java)
        .associateBy {
            val key = it.get().id
            // Adapted from KtLint CLI module
            if (key == "standard") "\u0000$key" else key
        }

    interface KtLintWorkParameters : WorkParameters {
        val filesToLint: ConfigurableFileCollection
        val android: Property<Boolean>
        val disabledRules: SetProperty<String>
        val enableExperimental: Property<Boolean>
        val debug: Property<Boolean>
        val additionalEditorconfigFile: RegularFileProperty
        val formatSource: Property<Boolean>
        val discoveredErrorsFile: RegularFileProperty
    }
}
