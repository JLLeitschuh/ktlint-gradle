package org.jlleitschuh.gradle.ktlint.worker

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jlleitschuh.gradle.ktlint.selectReportersLoaderAdapter
import java.io.File
import java.io.PrintStream

@Suppress("UnstableApiUsage")
internal abstract class GenerateBaselineWorkAction :
    WorkAction<GenerateBaselineWorkAction.GenerateBaselineParameters> {

    private val logger = Logging.getLogger("ktlint-generate-baseline-worker")

    override fun execute() {
        val ktLintClassesSerializer = KtLintClassesSerializer.create()

        val errors = parameters
            .discoveredErrors
            .files
            .filter { it.exists() }
            .map(ktLintClassesSerializer::loadErrors)
            .flatten()

        val baselineFile = parameters.baselineFile.asFile.get().apply {
            if (exists()) delete() else parentFile.mkdirs()
        }

        val projectDir = parameters.projectDirectory.asFile.get()

        PrintStream(baselineFile.outputStream()).use { file ->
            val baselineReporter = selectReportersLoaderAdapter(parameters.ktLintVersion.get())
                .loadAllGenericReporterProviders()
                .first { it.id == "baseline" }
                .get(file, emptyMap())

            baselineReporter.beforeAll()
            errors.forEach { lintErrorResult ->
                val filePath = lintErrorResult
                    .lintedFile
                    .toRelativeString(projectDir)
                    .replace(File.separatorChar, '/')

                baselineReporter.before(filePath)
                lintErrorResult.lintErrors.forEach {
                    baselineReporter.onLintError(filePath, it.first, it.second)
                }
                baselineReporter.after(filePath)
            }
            baselineReporter.afterAll()
        }

        logger.log(
            LogLevel.WARN,
            "Baseline was successfully generated into: ${parameters.baselineFile.get().asFile.absolutePath}"
        )
    }

    internal interface GenerateBaselineParameters : WorkParameters {
        val discoveredErrors: ConfigurableFileCollection
        val baselineFile: RegularFileProperty
        val projectDirectory: DirectoryProperty
        val ktLintVersion: Property<String>
    }
}
