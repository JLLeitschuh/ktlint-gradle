package org.jlleitschuh.gradle.ktlint.reporter

import java.io.File
import java.io.Serializable

/**
 * T is a ReporterProvider / ReporterProviderV2
 * GR is a generic wrapper for the reporter
 * GRP is the generic wrapper for the provider
 */
interface ReportersLoaderAdapter<
    R,
    RP : Serializable,
    GR : GenericReporter<R>,
    GRP : GenericReporterProvider<GR>> {
    fun loadAllReporterProviders(): List<ReporterProviderWrapper<RP>>
    fun filterEnabledBuiltInProviders(
        enabledReporters: Set<ReporterType>,
        allProviders: List<ReporterProviderWrapper<RP>>
    ): List<Pair<LoadedReporter, RP>>

    fun filterCustomProviders(
        customReporters: Set<CustomReporter>,
        allProviders: List<RP>
    ): List<Pair<LoadedReporter, RP>>

    fun allEnabledProviders(
        enabledReporters: Set<ReporterType>,
        customReporters: Set<CustomReporter>
    ): List<Pair<LoadedReporter, RP>> {
        val all = loadAllReporterProviders()
        return filterEnabledBuiltInProviders(enabledReporters, all)
            .plus(filterCustomProviders(customReporters, all.map { it.reporterProvider }))
    }

    fun loadReporterProviders(serializedReporterProviders: File): List<GRP>

    fun loadAllGenericReporterProviders(): List<GRP>
}
