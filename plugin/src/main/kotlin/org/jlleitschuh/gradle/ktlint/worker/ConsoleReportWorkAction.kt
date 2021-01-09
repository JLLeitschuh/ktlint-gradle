package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.LintError
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.util.prefixIfNot

@Suppress("UnstableApiUsage")
internal abstract class ConsoleReportWorkAction : WorkAction<ConsoleReportWorkAction.ConsoleReportParameters> {

    private val logger = Logging.getLogger("ktlint-console-report-worker")

    override fun execute() {
        val errors = loadErrors(parameters.discoveredErrors.asFile.get())

        if (parameters.outputToConsole.getOrElse(false)) {
            val verbose = parameters.verbose.get()
            errors.forEach { lintErrorResult ->
                val filePath = lintErrorResult.lintedFile.absolutePath
                lintErrorResult
                    .lintErrors
                    .filter { !it.second }
                    .forEach {
                        it.first.lintError.logError(filePath, verbose)
                    }
            }
        }

        if (errors.any { it.lintErrors.any { !it.second } } &&
            !parameters.ignoreFailures.getOrElse(false)
        ) {
            val reportsPaths = parameters
                .generatedReportsPaths
                .files.joinToString(separator = "\n") { it.absolutePath.prefixIfNot("|- ") }

            throw GradleException(
                """
                |KtLint found code style violations. You could find them in following reports:
                $reportsPaths
                """.trimMargin()
            )
        }
    }

    private fun LintError.logError(
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
    }
}
