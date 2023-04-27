package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.core.ReporterProvider
import java.io.File
import java.io.ObjectInputStream
import java.io.Serializable

/**
 * T is a ReporterProvider / ReporterProviderV2
 */
interface ReportersLoaderAdapter<T : Serializable> {
    fun loadAllReporterProviders(): List<ReporterProviderWrapper<T>>
    fun filterEnabledBuiltInProviders(
        enabledReporters: Set<ReporterType>,
        allProviders: List<ReporterProviderWrapper<T>>
    ): List<Pair<LoadedReporter, T>>


    fun filterCustomProviders(
        customReporters: Set<CustomReporter>,
        allProviders: List<T>
    ): List<Pair<LoadedReporter, T>>

    fun allEnabledProviders(
        enabledReporters: Set<ReporterType>,
        customReporters: Set<CustomReporter>
    ): List<Pair<LoadedReporter, T>> {
        val all = loadAllReporterProviders()
        return filterEnabledBuiltInProviders(enabledReporters, all)
            .plus(filterCustomProviders(customReporters, all.map { it.reporterProvider }))
    }

    fun loadReporterProviders(
        serializedReporterProviders: File
    ): List<GenericReporterProvider<*>>
}
