package org.jlleitschuh.gradle.ktlint.baseline

import com.pinterest.ktlint.cli.reporter.baseline.loadBaseline
import com.pinterest.ktlint.cli.reporter.core.api.KtlintCliError
import org.jlleitschuh.gradle.ktlint.worker.LintErrorResult
import org.jlleitschuh.gradle.ktlint.worker.SerializableLintError
import org.jlleitschuh.gradle.ktlint.worker.containsLintError
import org.jlleitschuh.gradle.ktlint.worker.toCliError
import java.io.File

class Ktlint1BaselineAdapter : BaselineAdapter<KtlintCliError> {
    var baselineRules: Map<String, List<KtlintCliError>> = emptyMap()
    override fun loadBaselineFile(baselineFile: File) {
        baselineRules = loadBaseline(baselineFile.absolutePath).lintErrorsPerFile
    }

    override fun filterOutBaseline(
        projectDir: File,
        lintErrorResult: LintErrorResult
    ): List<Pair<SerializableLintError, Boolean>> {
        val baselineLintErrors = baselineRules.getOrDefault(
            lintErrorResult.lintedFile.toRelativeString(projectDir).replace(File.separatorChar, '/'),
            emptyList()
        )
        return lintErrorResult.lintErrors.filter {
            !baselineLintErrors.containsLintError(it.first.toCliError())
        }
    }
}
