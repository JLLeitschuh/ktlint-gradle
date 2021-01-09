package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.ReporterProvider
import com.pinterest.ktlint.core.RuleSet
import org.apache.commons.io.serialization.ValidatingObjectInputStream
import java.io.File
import java.io.FileInputStream

internal fun loadErrors(
    serializedErrors: File
): List<LintErrorResult> = ValidatingObjectInputStream(FileInputStream(serializedErrors))
    .use {
        it.accept(
            ArrayList::class.java,
            LintErrorResult::class.java,
            File::class.java,
            Pair::class.java,
            SerializableLintError::class.java,
            java.lang.Boolean::class.java
        )
        it.accept("kotlin.Pair")
        @Suppress("UNCHECKED_CAST")
        it.readObject() as List<LintErrorResult>
    }

internal fun loadReporterProviders(
    serializedReporterProviders: File
): List<ReporterProvider> = ValidatingObjectInputStream(FileInputStream(serializedReporterProviders))
    .use {
        it.accept(
            ArrayList::class.java,
            SerializableReporterProvider::class.java
        )
        @Suppress("UNCHECKED_CAST")
        it.readObject() as List<SerializableReporterProvider>
    }
    .map { it.reporterProvider }

internal fun loadRuleSets(
    serializedRuleSets: File
): List<RuleSet> = ValidatingObjectInputStream(FileInputStream(serializedRuleSets))
    .use {
        it.accept(
            ArrayList::class.java,
            SerializableRuleSet::class.java,
        )
        @Suppress("UNCHECKED_CAST")
        it.readObject() as List<SerializableRuleSet>
    }
    .map { it.ruleSet }
