package org.jlleitschuh.gradle.ktlint.reporter

import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.ProblemReporter
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity
import org.jlleitschuh.gradle.ktlint.worker.SerializableLintError
import javax.inject.Inject

internal class ProblemsApiReporter @Inject constructor(
    private val problems: Problems
) {

    companion object {
        private val PROBLEM_GROUP = ProblemGroup.create("ktlint-gradle", "ktlint-gradle issue")
    }

    fun reportProblems(lintErrors: Map<String, List<SerializableLintError>>, ignoreFailures: Boolean) {
        val severity = if (ignoreFailures) Severity.WARNING else Severity.ERROR
        lintErrors.forEach { (filePath, errors) ->
            errors.forEach { error ->
                reportProblem(error, filePath, severity)
            }
        }
    }

    fun reportProblem(error: SerializableLintError, filePath: String, severity: Severity) {
        val reporter: ProblemReporter? = problems.reporter
        val id = ProblemId.create(error.ruleId, error.detail, PROBLEM_GROUP)
        reporter?.report(id) {
            lineInFileLocation(filePath, error.line, error.col)
            details(error.detail)
            severity(severity)
            solution("Run ktlintFormat to auto-fix this issue")
        }
    }
}
