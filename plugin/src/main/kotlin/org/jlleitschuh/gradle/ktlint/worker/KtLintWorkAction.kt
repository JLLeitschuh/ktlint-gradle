package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.RuleSet
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@Suppress("UnstableApiUsage")
abstract class KtLintWorkAction : WorkAction<KtLintWorkAction.KtLintWorkParameters> {
    override fun execute() {
        val serializedRuleSets = parameters.loadedRuleSets.asFile.get()
        val ruleSets = ObjectInputStream(FileInputStream(serializedRuleSets))
            .use {
                @Suppress("UNCHECKED_CAST")
                it.readObject() as List<SerializableRuleSet>
            }
            .map { it.ruleSet }

        val additionalEditorConfig = parameters
            .additionalEditorconfigFile
            .orNull
            ?.asFile
            ?.absolutePath
        val userData = generateUserData(ruleSets)
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

    private fun generateUserData(loadedRuleSets: List<RuleSet>): Map<String, String> {
        val userData = mutableMapOf(
            "android" to parameters.android.get().toString()
        )
        val disabledRules = parameters.disabledRules.get()
        if (disabledRules.isNotEmpty()) {
            val standardRuleSet = loadedRuleSets.find { it.id == "standard" }
            userData["disabled_rules"] = disabledRules.joinToString(
                separator = ","
            ) { disabledRuleId ->
                // Workaround for '===' usage on this line:
                // https://github.com/pinterest/ktlint/blob/fc64c4ff2d7179ae4fcf7cac2691fafbec55a552/ktlint-core/src/main/kotlin/com/pinterest/ktlint/core/KtLint.kt#L219
                // Loaded standard ruleset id is not "===" to "standard" string from ktlint jar.
                if (standardRuleSet != null && standardRuleSet.rules.any { it.id == disabledRuleId }) {
                    "standard:$disabledRuleId"
                } else {
                    disabledRuleId
                }
            }
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
