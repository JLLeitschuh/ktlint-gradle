package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import java.io.File
import java.io.ObjectInputStream
import java.util.ServiceLoader

class ReportersProviderLoader :
    ReportersLoaderAdapter<Reporter, ReporterProvider, Ktlint41Reporter, Ktlint41ReporterProvider> {
    override fun loadAllReporterProviders(): List<ReporterProviderWrapper<ReporterProvider>> = ServiceLoader
        .load(ReporterProvider::class.java)
        .toList().map {
            ReporterProviderWrapper(it.id, it)
        }

    override fun loadAllGenericReporterProviders(): List<Ktlint41ReporterProvider> = ServiceLoader
        .load(ReporterProvider::class.java)
        .toList().map {
            Ktlint41ReporterProvider(it)
        }

    override fun loadReporterProviders(serializedReporterProviders: File): List<Ktlint41ReporterProvider> {
        return ObjectInputStream(
            serializedReporterProviders.inputStream().buffered()
        ).use {
            @Suppress("UNCHECKED_CAST")
            it.readObject() as List<ReporterProvider>
        }.map { Ktlint41ReporterProvider(it) }
    }

    override fun filterEnabledBuiltInProviders(
        enabledReporters: Set<ReporterType>,
        allProviders: List<ReporterProviderWrapper<ReporterProvider>>
    ): List<Pair<LoadedReporter, ReporterProvider>> {
        val enabledProviders = allProviders
            .filter { reporterProvider ->
                enabledReporters.any {
                    reporterProvider.id == it.reporterName
                }
            }
        return enabledReporters
            .map { reporterType ->
                val provider = enabledProviders.find { reporterType.reporterName == it.id }
                    ?: throw RuntimeException(
                        "KtLint plugin failed to load reporter ${reporterType.reporterName}."
                    )

                val options = if (reporterType == ReporterType.PLAIN_GROUP_BY_FILE) {
                    reporterType.options.associateWith { "true" }
                } else {
                    emptyMap()
                }

                LoadedReporter(provider.id, reporterType.fileExtension, options) to provider.reporterProvider
            }
    }

    override fun filterCustomProviders(
        customReporters: Set<CustomReporter>,
        allProviders: List<ReporterProvider>
    ): List<Pair<LoadedReporter, ReporterProvider>> {
        val customProviders = allProviders
            .filter { reporterProvider ->
                customReporters.any { reporterProvider.id == it.reporterId }
            }

        return customReporters
            .map { customReporter ->
                val provider = customProviders.find { customReporter.reporterId == it.id }
                    ?: throw RuntimeException(
                        "KtLint plugin failed to load ${customReporter.reporterId} custom reporter."
                    )
                LoadedReporter(customReporter.reporterId, customReporter.fileExtension, emptyMap()) to provider
            }
    }
}
