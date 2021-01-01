package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.util.ServiceLoader

/**
 * Preloads KtLint [RuleSet]s and serialize them into [RuleSetLoaderWorkParameters.serializeResultIntoFile] file,
 * so other [WorkAction] could use it.
 */
@Suppress("UnstableApiUsage")
internal abstract class RuleSetLoaderWorkAction :
    WorkAction<RuleSetLoaderWorkAction.RuleSetLoaderWorkParameters> {

    override fun execute() {
        val ruleSets = loadRuleSetsAndFilterThem(
            parameters.enableExperimentalRules.get(),
            parameters.disabledRules.get()
        )

        val fileSerializeInto = parameters.serializeResultIntoFile.asFile.get()
        ObjectOutputStream(FileOutputStream(fileSerializeInto)).use { oos ->
            oos.writeObject(
                ruleSets.map { SerializableRuleSet(it) }
            )
        }
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

    @Suppress("UnstableApiUsage")
    interface RuleSetLoaderWorkParameters : WorkParameters {
        val serializeResultIntoFile: RegularFileProperty
        val enableExperimentalRules: Property<Boolean>
        val disabledRules: SetProperty<String>
    }
}
