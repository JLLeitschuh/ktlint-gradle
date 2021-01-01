package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.KtLint
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
        val userData = generateUserData()
        val debug = parameters.debug.get()
        val formatSource = parameters.formatSource.getOrElse(false)

        val errors = mutableMapOf<SerializableLintError, Boolean>()

        parameters.filesToLint.files.forEach {
            val ktlintParameters = KtLint.Params(
                fileName = it.absolutePath,
                text = it.readText(),
                ruleSets = ruleSets,
                userData = userData,
                debug = debug,
                editorConfigPath = additionalEditorConfig,
                script = !it.name.endsWith(".kt", ignoreCase = true),
                cb = { lintError, isCorrected ->
                    errors[SerializableLintError(lintError)] = isCorrected
                }
            )

            if (formatSource) {
                KtLint.format(ktlintParameters)
            } else {
                KtLint.lint(ktlintParameters)
            }
        }

        ObjectOutputStream(
            FileOutputStream(
                parameters.discoveredErrorsFile.asFile.get()
            )
        ).use {
            it.writeObject(errors)
        }
    }

    private fun generateUserData(): Map<String, String> {
        val userData = mutableMapOf(
            "android" to parameters.android.get().toString()
        )
        val disabledRules = parameters.disabledRules.get()
        if (disabledRules.isNotEmpty()) {
            userData["disabled_rules"] = disabledRules.joinToString(
                separator = ","
            )
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
