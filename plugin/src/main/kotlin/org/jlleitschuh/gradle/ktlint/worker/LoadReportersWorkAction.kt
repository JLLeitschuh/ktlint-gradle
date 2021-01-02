package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.ReporterProvider
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jlleitschuh.gradle.ktlint.logKtLintDebugMessage
import org.jlleitschuh.gradle.ktlint.reporter.CustomReporter
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.util.ServiceLoader

@Suppress("UnstableApiUsage")
internal abstract class LoadReportersWorkAction : WorkAction<LoadReportersWorkAction.LoadReportersParameters> {

    private val logger = Logging.getLogger("ktlint-load-reporters-worker")

    override fun execute() {
        val allReporters = loadAllReporterProviders()
        val enabledReporters = getEnabledReporters()
        val disabledReporters = ReporterType.values().subtract(enabledReporters)
        val customReporters = parameters.customReporters.get()

        val loadedReporters = allReporters
            .filterNot { reporterProvider ->
                disabledReporters.any {
                    reporterProvider.id == it.reporterName
                }
            }
            .associateWith { reporterProvider ->
                val fileExtension = enabledReporters.find { it.reporterName == reporterProvider.id }?.fileExtension
                    ?: customReporters.find { it.reporterId == reporterProvider.id }?.fileExtension
                requireNotNull(fileExtension) { "Unknown ReporterProvider: \"${reporterProvider.id}\"!" }

                fileExtension
            }
            .mapKeys { SerializableReporterProvider(it.key) }

        ObjectOutputStream(
            FileOutputStream(
                parameters.loadedReporters.asFile.get()
            )
        ).use {
            it.writeObject(loadedReporters)
        }
    }

    private fun getEnabledReporters() = parameters
        .enabledReporters
        .get()
        .run {
            if (isEmpty()) setOf(ReporterType.PLAIN) else this
        }

    private fun loadAllReporterProviders(): List<ReporterProvider> = ServiceLoader
        .load(ReporterProvider::class.java)
        .toList()
        .also { reporterProviders ->
            logger.logKtLintDebugMessage(
                parameters.debug.getOrElse(false)
            ) {
                reporterProviders.map { "Discovered reporter with \"${it.id}\" id." }
            }
        }

    internal interface LoadReportersParameters : WorkParameters {
        val enabledReporters: SetProperty<ReporterType>
        val customReporters: SetProperty<CustomReporter>
        val debug: Property<Boolean>
        val loadedReporters: RegularFileProperty
    }
}
