package org.jlleitschuh.gradle.ktlint.worker

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.PrintStream

@Suppress("UnstableApiUsage")
internal abstract class GenerateReportsWorkAction : WorkAction<GenerateReportsWorkAction.GenerateReportsParameters> {

    override fun execute() {
        val discoveredErrors = loadErrors(parameters.discoveredErrorsFile.get().asFile)
        val currentReporterId = parameters.reporterId.get()
        val reporterProvider = loadReporterProviders(parameters.loadedReporterProviders.asFile.get())
            .find { it.id == currentReporterId }
            ?: throw GradleException("Could not find ReporterProvider \"$currentReporterId\"")

        PrintStream(
            parameters
                .reporterOutput
                .get()
                .asFile
        ).use { printStream ->
            val reporter = reporterProvider.get(printStream, parameters.reporterOptions.get())

            reporter.beforeAll()
            discoveredErrors.forEach { lintErrorResult ->
                val filePath = lintErrorResult.lintedFile.absolutePath
                reporter.before(filePath)
                lintErrorResult.lintErrors.forEach {
                    reporter.onLintError(filePath, it.first.lintError, it.second)
                }
                reporter.after(filePath)
            }
            reporter.afterAll()
        }
    }

    internal interface GenerateReportsParameters : WorkParameters {
        val discoveredErrorsFile: RegularFileProperty
        val loadedReporterProviders: RegularFileProperty
        val reporterId: Property<String>
        val reporterOutput: RegularFileProperty
        val reporterOptions: MapProperty<String, String>
    }
}
