package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.util.prefixIfNot
import org.jlleitschuh.gradle.ktlint.reporter.ProblemsApiReporter
import java.io.File
import javax.inject.Inject

internal abstract class ConsoleReportWorkAction : WorkAction<ConsoleReportWorkAction.ConsoleReportParameters> {

    private val logger = Logging.getLogger("ktlint-console-report-worker")

    @Inject
    private lateinit var problemsApiReporter: ProblemsApiReporter

    override fun execute() {
        val errors = KtLintClassesSerializer
            .create()
            .loadErrors(
                parameters.discoveredErrors.asFile.get()
            )

        val baselineRules = parameters.baseline.orNull?.asFile?.absolutePath
            ?.let { loadBaseline(it).lintErrorsPerFile }
        val projectDir = parameters.projectDirectory.asFile.get()

        val lintErrors = errors.associate { lintErrorResult ->
            val filePath = lintErrorResult.lintedFile.absolutePath
            val baselineLintErrors = baselineRules?.get(
                lintErrorResult.lintedFile.toRelativeString(projectDir).replace(File.separatorChar, '/')
            )
            filePath to lintErrorResult
                .lintErrors
                .filter {
                    !it.second &&
                        baselineLintErrors?.containsLintError(it.first.toCliError()) != true
                }
                .map { it.first }
        }

        val isLintErrorsFound = lintErrors.values.flatten().isNotEmpty()

        // Report problems to Gradle Problems API if available
        if (isLintErrorsFound) {
            try {
                problemsApiReporter.reportProblems(lintErrors, parameters.ignoreFailures.getOrElse(false))
            } catch (e: Exception) {
                // Problems API might not be available in all Gradle versions
                logger.debug("Problems API not available: ${e.message}")
            }
        }

        if (parameters.outputToConsole.getOrElse(false) && isLintErrorsFound) {
            val verbose = parameters.verbose.get()
            lintErrors.forEach { (filePath, errors) ->
                errors.forEach { it.logError(filePath, verbose) }
            }
        }

        if (!parameters.ignoreFailures.getOrElse(false) && isLintErrorsFound) {
            val reportsPaths = parameters
                .generatedReportsPaths
                .files.joinToString(separator = "\n") { it.absolutePath.prefixIfNot("|- ") }

            throw GradleException(
                """
                |KtLint found code style violations. Please see the following reports:
                $reportsPaths
                """.trimMargin()
            )
        }
    }

    private fun SerializableLintError.logError(
        filePath: String,
        verbose: Boolean
    ) {
        val verboseSuffix = if (verbose) " ($ruleId)" else ""
        val errorDetail = if (!canBeAutoCorrected) {
            "$detail (cannot be auto-corrected)"
        } else {
            detail
        }
        logger.warn("$filePath:$line:$col $errorDetail$verboseSuffix")
    }

    internal interface ConsoleReportParameters : WorkParameters {
        val discoveredErrors: RegularFileProperty
        val outputToConsole: Property<Boolean>
        val ignoreFailures: Property<Boolean>
        val verbose: Property<Boolean>
        val generatedReportsPaths: ConfigurableFileCollection
        val ktLintVersion: Property<String>
        val baseline: RegularFileProperty
        val projectDirectory: DirectoryProperty
    }
}
