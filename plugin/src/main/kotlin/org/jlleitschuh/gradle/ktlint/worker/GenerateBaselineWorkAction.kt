package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import net.swiftzer.semver.SemVer
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.io.File
import java.io.PrintStream
import java.util.ServiceLoader

@Suppress("UnstableApiUsage")
internal abstract class GenerateBaselineWorkAction :
    WorkAction<GenerateBaselineWorkAction.GenerateBaselineParameters> {

    private val logger = Logging.getLogger("ktlint-generate-baseline-worker")

    override fun execute() {
        val ktLintClassesSerializer = KtLintClassesSerializer.create(
            SemVer.parse(parameters.ktLintVersion.get())
        )
        val errors = parameters
            .discoveredErrors
            .files
            .filter { it.exists() }
            .map {
                ktLintClassesSerializer.loadErrors(it)
            }
            .flatten()

        val baselineFile = parameters.baselineFile.asFile.get().apply {
            if (exists()) delete() else parentFile.mkdirs()
        }
        val projectDir = parameters.projectDirectory.asFile.get()

        PrintStream(baselineFile.outputStream()).use { file ->
            val baselineReporter = loadBaselineReporter(file)
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

    private fun loadBaselineReporter(
        baselineFile: PrintStream
    ): Reporter = ServiceLoader
        .load(ReporterProvider::class.java)
        .first { it.id == "baseline" }
        .get(baselineFile, emptyMap())

    internal interface GenerateBaselineParameters : WorkParameters {
        val discoveredErrors: ConfigurableFileCollection
        val baselineFile: RegularFileProperty
        val projectDirectory: DirectoryProperty
        val ktLintVersion: Property<String>
    }
}
