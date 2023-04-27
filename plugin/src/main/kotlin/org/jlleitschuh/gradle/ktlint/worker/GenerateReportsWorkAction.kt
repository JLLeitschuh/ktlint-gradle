package org.jlleitschuh.gradle.ktlint.worker

import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jlleitschuh.gradle.ktlint.selectBaselineLoader
import org.jlleitschuh.gradle.ktlint.selectReportersLoaderAdapter
import java.io.File
import java.io.PrintStream

@Suppress("UnstableApiUsage")
internal abstract class GenerateReportsWorkAction : WorkAction<GenerateReportsWorkAction.GenerateReportsParameters> {

    override fun execute() {
        val baselineLoader = selectBaselineLoader(parameters.ktLintVersion.get())
        val ktLintClassesSerializer = KtLintClassesSerializer
            .create(
                SemVer.parse(parameters.ktLintVersion.get())
            )

        val discoveredErrors = ktLintClassesSerializer.loadErrors(parameters.discoveredErrorsFile.get().asFile)
        val currentReporterId = parameters.reporterId.get()
        val reporterAdapter = selectReportersLoaderAdapter(parameters.ktLintVersion.get())
        val reporterProvider = reporterAdapter.loadReporterProviders(parameters.loadedReporterProviders.asFile.get())
            .find { it.id == currentReporterId }
            ?: throw GradleException("Could not find ReporterProvider \"$currentReporterId\"")

        val baselineRules: Map<String, List<SerializableLintError>>? = parameters.baseline.orNull?.asFile?.absolutePath
            ?.let { baselineLoader.loadBaselineRules(it) }
        val projectDir = parameters.projectDirectory.asFile.get()

        PrintStream(
            parameters
                .reporterOutput
                .get()
                .asFile
        ).use { printStream ->
            val reporter = reporterProvider.get(printStream, parameters.reporterOptions.get())

            reporter.beforeAll()
            discoveredErrors.forEach { lintErrorResult ->
                val filePath = filePathForReport(lintErrorResult.lintedFile)
                val baselineLintErrors = baselineRules?.get(
                    lintErrorResult.lintedFile.toRelativeString(projectDir).replace(File.separatorChar, '/')
                )
                reporter.before(filePath)
                lintErrorResult.lintErrors.forEach {
                    if (baselineLintErrors?.containsLintError(it.first) != true) {
                        reporter.onLintError(filePath, it.first, it.second)
                    }
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
