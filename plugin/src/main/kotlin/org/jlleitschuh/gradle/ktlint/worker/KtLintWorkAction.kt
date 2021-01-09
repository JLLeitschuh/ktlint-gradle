package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.RuleSet
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.FileOutputStream
import java.io.ObjectOutputStream

@Suppress("UnstableApiUsage")
abstract class KtLintWorkAction : WorkAction<KtLintWorkAction.KtLintWorkParameters> {
    override fun execute() {
        val ruleSets = loadRuleSets(parameters.loadedRuleSets.get().asFile)
            .fixStandardRuleSet()

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
            val ktlintParameters = KtLint.Params(
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
                val updatedFileContent = KtLint.format(ktlintParameters)

                if (updatedFileContent != currentFileContent) {
                    it.writeText(updatedFileContent)
                }
            } else {
                KtLint.lint(ktlintParameters)
            }
            result.add(
                LintErrorResult(
                    lintedFile = it,
                    lintErrors = errors
                )
            )
        }

        ObjectOutputStream(
            FileOutputStream(
                parameters.discoveredErrorsFile.asFile.get()
            )
        ).use {
            it.writeObject(result)
        }
    }

    /**
     * Applies workaround for '===' usage on this line:
     * https://github.com/pinterest/ktlint/blob/fc64c4ff2d7179ae4fcf7cac2691fafbec55a552/ktlint-core/src/main/kotlin/com/pinterest/ktlint/core/KtLint.kt#L219
     * Loaded standard ruleset id is not "===" to "standard" string from ktlint jar.
     */
    private fun List<RuleSet>.fixStandardRuleSet(): List<RuleSet> {
        val standardRuleSet = find { it.id == "standard" } ?: return this

        val standardRuleSetId = "standard".intern()
        val newStandardRuleSet = RuleSet(
            standardRuleSetId,
            *standardRuleSet.rules
        )

        return filter { it != standardRuleSet }.plus(listOf(newStandardRuleSet))
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

    interface KtLintWorkParameters : WorkParameters {
        val loadedRuleSets: RegularFileProperty
        val filesToLint: ConfigurableFileCollection
        val android: Property<Boolean>
        val disabledRules: SetProperty<String>
        val debug: Property<Boolean>
        val additionalEditorconfigFile: RegularFileProperty
        val formatSource: Property<Boolean>
        val discoveredErrorsFile: RegularFileProperty
    }
}
