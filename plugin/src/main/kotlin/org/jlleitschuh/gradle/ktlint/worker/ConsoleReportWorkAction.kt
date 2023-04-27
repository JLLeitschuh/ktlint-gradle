package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.LintError
import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.util.prefixIfNot
import org.jlleitschuh.gradle.ktlint.selectBaselineLoader
import java.io.File

@Suppress("UnstableApiUsage")
internal abstract class ConsoleReportWorkAction : WorkAction<ConsoleReportWorkAction.ConsoleReportParameters> {

    private val logger = Logging.getLogger("ktlint-console-report-worker")

    override fun execute() {
        val baselineLoader = selectBaselineLoader(parameters.ktLintVersion.get())
        val errors = KtLintClassesSerializer
            .create(
                SemVer.parse(parameters.ktLintVersion.get())
            )
            .loadErrors(
                parameters.discoveredErrors.asFile.get()
            )

        val baselineRules = parameters.baseline.orNull?.asFile?.absolutePath
            ?.let { baselineLoader.loadBaselineRules(it) }
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
                        baselineLintErrors?.containsLintError(it.first) != true
                }
                .map { it.first }
        }

        val isLintErrorsFound = lintErrors.values.flatten().isNotEmpty()
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
