package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.ReporterProvider
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jlleitschuh.gradle.ktlint.logKtLintDebugMessage
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.util.ServiceLoader

@Suppress("UnstableApiUsage")
internal abstract class LoadReportersWorkAction : WorkAction<LoadReportersWorkAction.LoadReportersParameters> {

    private val logger = Logging.getLogger("ktlint-load-reporters-worker")

    override fun execute() {
        val allReporters = loadAllReporterProviders()
        logger.logKtLintDebugMessage(
            parameters.debug.getOrElse(false)
        ) {
            allReporters.map { "Discovered reporter with \"${it.id}\" id." }
        }

        val enabledReporters = parameters
            .enabledReporters
            .get()
            .run {
                if (isEmpty()) setOf(ReporterType.PLAIN) else this
            }
        val disabledReporters = ReporterType.values().subtract(enabledReporters)
        val filteredReporters = allReporters
            .filterNot { reporterProvider ->
                disabledReporters.any {
                    reporterProvider.id == it.reporterName
                }
            }
            .map { SerializableReporterProvider(it) }

        ObjectOutputStream(
            FileOutputStream(
                parameters.loadedReporters.asFile.get()
            )
        ).use {
            it.writeObject(filteredReporters)
        }
    }

    private fun loadAllReporterProviders(): List<ReporterProvider> = ServiceLoader
        .load(ReporterProvider::class.java)
        .toList()

    internal interface LoadReportersParameters : WorkParameters {
        val enabledReporters: SetProperty<ReporterType>
        val debug: Property<Boolean>
        val loadedReporters: RegularFileProperty
    }
}
