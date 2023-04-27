package org.jlleitschuh.gradle.ktlint.worker

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jlleitschuh.gradle.ktlint.reporter.CustomReporter
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jlleitschuh.gradle.ktlint.selectReportersLoaderAdapter
import java.io.ObjectOutputStream

@Suppress("UnstableApiUsage")
internal abstract class LoadReportersWorkAction : WorkAction<LoadReportersWorkAction.LoadReportersParameters> {
    private val logger = Logging.getLogger("ktlint-load-reporters-worker")

    override fun execute() {
        val reportersLoaderAdapter = selectReportersLoaderAdapter(parameters.ktLintVersion.get())
        val loadedReporters = reportersLoaderAdapter.allEnabledProviders(
            getEnabledReporters(),
            parameters.customReporters.get()
        )

        val ktLintClassesSerializer = KtLintClassesSerializer.create()
        ktLintClassesSerializer.saveReporterProviders(
            loadedReporters.map { it.second },
            parameters.loadedReporterProviders.asFile.get()
        )

        ObjectOutputStream(
            parameters.loadedReporters.asFile.get().outputStream().buffered()
        ).use { oos ->
            oos.writeObject(
                loadedReporters.map { it.first }
            )
        }
    }

    private fun getEnabledReporters() = parameters
        .enabledReporters
        .get()
        .run {
            if (isEmpty()) setOf(ReporterType.PLAIN) else this
        }
    internal interface LoadReportersParameters : WorkParameters {
        val enabledReporters: SetProperty<ReporterType>
        val customReporters: SetProperty<CustomReporter>
        val debug: Property<Boolean>
        val loadedReporterProviders: RegularFileProperty
        val loadedReporters: RegularFileProperty
        val ktLintVersion: Property<String>
    }
}
