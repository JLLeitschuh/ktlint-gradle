package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.cli.reporter.core.api.ReporterProviderV2
import com.pinterest.ktlint.cli.reporter.core.api.ReporterV2
import java.io.File
import java.io.ObjectInputStream
import java.util.ServiceLoader

class ReportersProviderV2Loader :
    ReportersLoaderAdapter<ReporterV2, ReporterProviderV2<*>, ReporterV2Reporter, ReporterProviderV2ReporterProvider> {
    override fun loadAllReporterProviders(): List<ReporterProviderWrapper<ReporterProviderV2<*>>> = ServiceLoader
        .load(ReporterProviderV2::class.java)
        .toList().map {
            ReporterProviderWrapper(it.id, it)
        }

    override fun loadReporterProviders(serializedReporterProviders: File): List<ReporterProviderV2ReporterProvider> {
        return ObjectInputStream(
            serializedReporterProviders.inputStream().buffered()
        ).use {
            @Suppress("UNCHECKED_CAST")
            it.readObject() as List<ReporterProviderV2<ReporterV2>>
        }.map { ReporterProviderV2ReporterProvider(it) }
    }

    override fun loadAllGenericReporterProviders(): List<ReporterProviderV2ReporterProvider> = ServiceLoader
        .load(ReporterProviderV2::class.java)
        .toList().map {
            ReporterProviderV2ReporterProvider(it)
        }

    override fun filterCustomProviders(
        customReporters: Set<CustomReporter>,
        allProviders: List<ReporterProviderV2<*>>
    ): List<Pair<LoadedReporter, ReporterProviderV2<*>>> {
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

    override fun filterEnabledBuiltInProviders(
        enabledReporters: Set<ReporterType>,
        allProviders: List<ReporterProviderWrapper<ReporterProviderV2<*>>>
    ): List<Pair<LoadedReporter, ReporterProviderV2<*>>> {
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
}
