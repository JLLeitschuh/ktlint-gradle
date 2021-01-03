package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.ReporterProvider
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.io.PrintStream

@Suppress("UnstableApiUsage")
internal abstract class GenerateReportsWorkAction : WorkAction<GenerateReportsWorkAction.GenerateReportsParameters> {

    override fun execute() {
        val discoveredErrors = loadErrors()
        val currentReporterId = parameters.reporterId.get()
        val reporterProvider = loadReporterProviders().find { it.id == currentReporterId }
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

    private fun loadReporterProviders(): List<ReporterProvider> =
        ObjectInputStream(FileInputStream(parameters.loadedReporterProviders.asFile.get()))
            .use {
                @Suppress("UNCHECKED_CAST")
                it.readObject() as List<SerializableReporterProvider>
            }
            .map { it.reporterProvider }

    private fun loadErrors(): List<LintErrorResult> =
        ObjectInputStream(FileInputStream(parameters.discoveredErrorsFile.asFile.get()))
            .use {
                @Suppress("UNCHECKED_CAST")
                it.readObject() as List<LintErrorResult>
            }

    internal interface GenerateReportsParameters : WorkParameters {
        val discoveredErrorsFile: RegularFileProperty
        val loadedReporterProviders: RegularFileProperty
        val reporterId: Property<String>
        val reporterOutput: RegularFileProperty
        val reporterOptions: MapProperty<String, String>
    }
}
