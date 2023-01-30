package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.ReporterProvider
import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jlleitschuh.gradle.ktlint.logKtLintDebugMessage
import org.jlleitschuh.gradle.ktlint.reporter.CustomReporter
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.ServiceLoader

@Suppress("UnstableApiUsage")
internal abstract class LoadReportersWorkAction : WorkAction<LoadReportersWorkAction.LoadReportersParameters> {

    private val logger = Logging.getLogger("ktlint-load-reporters-worker")

    override fun execute() {
        val allProviders = loadAllReporterProviders()
        val loadedReporters = filterEnabledBuiltInProviders(allProviders) + filterCustomProviders(allProviders)

        val ktLintClassesSerializer = KtLintClassesSerializer.create(
            SemVer.parse(parameters.ktLintVersion.get())
        )
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

    private fun loadAllReporterProviders(): List<ReporterProvider<*>> = ServiceLoader
        .load(ReporterProvider::class.java)
        .toList()
        .also { reporterProviders ->
            logger.logKtLintDebugMessage(
                parameters.debug.getOrElse(false)
            ) {
                reporterProviders.map { "Discovered reporter with \"${it.id}\" id." }
            }
        }

    private fun filterEnabledBuiltInProviders(
        allProviders: List<ReporterProvider<*>>
    ): List<Pair<LoadedReporter, ReporterProvider<*>>> {
        val enabledReporters = getEnabledReporters()

        val enabledProviders = allProviders
            .filter { reporterProvider ->
                enabledReporters.any {
                    reporterProvider.id == it.reporterName
                }
            }
        return enabledReporters
            .map { reporterType ->
                val provider = enabledProviders.find { reporterType.reporterName == it.id }
                    ?: throw GradleException(
                        "KtLint plugin failed to load reporter ${reporterType.reporterName}."
                    )

                val options = if (reporterType == ReporterType.PLAIN_GROUP_BY_FILE) {
                    reporterType.options.associateWith { "true" }
                } else {
                    emptyMap()
                }

                LoadedReporter(provider.id, reporterType.fileExtension, options) to provider
            }
    }

    private fun filterCustomProviders(
        allProviders: List<ReporterProvider<*>>
    ): List<Pair<LoadedReporter, ReporterProvider<*>>> {
        val customReporters = parameters.customReporters.get()
        val customProviders = allProviders
            .filter { reporterProvider ->
                customReporters.any { reporterProvider.id == it.reporterId }
            }

        return customReporters
            .map { customReporter ->
                val provider = customProviders.find { customReporter.reporterId == it.id }
                    ?: throw GradleException(
                        "KtLint plugin failed to load ${customReporter.reporterId} custom reporter."
                    )
                LoadedReporter(customReporter.reporterId, customReporter.fileExtension, emptyMap()) to provider
            }
    }

    internal interface LoadReportersParameters : WorkParameters {
        val enabledReporters: SetProperty<ReporterType>
        val customReporters: SetProperty<CustomReporter>
        val debug: Property<Boolean>
        val loadedReporterProviders: RegularFileProperty
        val loadedReporters: RegularFileProperty
        val ktLintVersion: Property<String>
    }

    internal data class LoadedReporter(
        val reporterId: String,
        val fileExtension: String,
        val reporterOptions: Map<String, String>
    ) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 201201233L
        }
    }
}
