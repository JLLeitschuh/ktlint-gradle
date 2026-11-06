package org.jlleitschuh.gradle.ktlint.worker

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jlleitschuh.gradle.ktlint.selectBaselineAdapter
import org.jlleitschuh.gradle.ktlint.selectReportersLoaderAdapter
import java.io.File
import java.io.PrintStream

internal abstract class GenerateReportsWorkAction : WorkAction<GenerateReportsWorkAction.GenerateReportsParameters> {

    override fun execute() {
        val ktLintClassesSerializer = KtLintClassesSerializer
            .create()

        val discoveredErrors = if (parameters.discoveredErrorsFile.get().asFile.exists()) {
            ktLintClassesSerializer.loadErrors(parameters.discoveredErrorsFile.get().asFile)
        } else {
            emptyList()
        }
        val currentReporterId = parameters.reporterId.get()
        val reporterAdapter = selectReportersLoaderAdapter(parameters.ktLintVersion.get())
        val reporterProvider = reporterAdapter.loadReporterProviders(parameters.loadedReporterProviders.asFile.get())
            .find { it.id == currentReporterId }
            ?: throw GradleException("Could not find ReporterProvider \"$currentReporterId\"")

        val baselineAdapter = selectBaselineAdapter(parameters.ktLintVersion.get())
        parameters.baseline.orNull?.asFile
            ?.let { baselineAdapter.loadBaselineFile(it) }

        PrintStream(parameters.reporterOutput.get().asFile).use { printStream ->
            val reporter = reporterProvider.get(printStream, parameters.reporterOptions.get())

            reporter.beforeAll()
            discoveredErrors.forEach { lintErrorResult ->
                val filePath = filePathForReport(lintErrorResult.lintedFile)
                reporter.before(filePath)
                baselineAdapter.filterOutBaseline(parameters.projectDirectory.asFile.get(), lintErrorResult).forEach {
                    reporter.onLintError(filePath, it.first, it.second)
                }
                reporter.after(filePath)
            }
            reporter.afterAll()
        }
    }

    private fun filePathForReport(file: File): String {
        val rootDir = parameters.filePathsRelativeTo.orNull
        if (rootDir != null) {
            return file.toRelativeString(rootDir)
        }

        return file.absolutePath
    }

    internal interface GenerateReportsParameters : WorkParameters {
        val discoveredErrorsFile: RegularFileProperty
        val loadedReporterProviders: RegularFileProperty
        val reporterId: Property<String>
        val reporterOutput: RegularFileProperty
        val reporterOptions: MapProperty<String, String>
        val ktLintVersion: Property<String>
        val baseline: RegularFileProperty
        val projectDirectory: DirectoryProperty
        val filePathsRelativeTo: Property<File>
    }
}
