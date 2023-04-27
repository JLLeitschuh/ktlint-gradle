package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import java.io.File
import java.io.ObjectInputStream
import java.util.ServiceLoader

class SerializableReportersProviderLoader :
    ReportersLoaderAdapter<Reporter, SerializableReporterProvider, Ktlint34Reporter, Ktlint34ReporterProvider> {
    override fun loadAllReporterProviders(): List<ReporterProviderWrapper<SerializableReporterProvider>> = ServiceLoader
        .load(ReporterProvider::class.java)
        .toList().map {
            ReporterProviderWrapper(it.id, SerializableReporterProvider(it))
        }

    override fun loadReporterProviders(serializedReporterProviders: File): List<Ktlint34ReporterProvider> {
        return ObjectInputStream(
            serializedReporterProviders.inputStream().buffered()
        ).use {
            @Suppress("UNCHECKED_CAST")
            it.readObject() as List<SerializableReporterProvider>
        }.map { Ktlint34ReporterProvider(it.reporterProvider) }
    }

    override fun loadAllGenericReporterProviders(): List<Ktlint34ReporterProvider> = ServiceLoader
        .load(ReporterProvider::class.java)
        .toList().map {
            Ktlint34ReporterProvider(it)
        }

    override fun filterEnabledBuiltInProviders(
        enabledReporters: Set<ReporterType>,
        allProviders: List<ReporterProviderWrapper<SerializableReporterProvider>>
    ): List<Pair<LoadedReporter, SerializableReporterProvider>> {
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
        allProviders: List<SerializableReporterProvider>
    ): List<Pair<LoadedReporter, SerializableReporterProvider>> {
        val customProviders = allProviders
            .filter { reporterProvider ->
                customReporters.any { reporterProvider.reporterProvider.id == it.reporterId }
            }

        return customReporters
            .map { customReporter ->
                val provider = customProviders.find { customReporter.reporterId == it.reporterProvider.id }
                    ?: throw RuntimeException(
                        "KtLint plugin failed to load ${customReporter.reporterId} custom reporter."
                    )
                LoadedReporter(customReporter.reporterId, customReporter.fileExtension, emptyMap()) to provider
            }
    }
}
