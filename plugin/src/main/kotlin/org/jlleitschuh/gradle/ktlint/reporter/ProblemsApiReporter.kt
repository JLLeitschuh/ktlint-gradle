package org.jlleitschuh.gradle.ktlint.reporter

import org.gradle.api.Incubating
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.ProblemReporter
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity
import org.jlleitschuh.gradle.ktlint.worker.SerializableLintError
import javax.inject.Inject

@Incubating
class ProblemsApiReporter @Inject constructor(
    private val problems: Problems,
) {

    fun reportProblems(lintErrors: Map<String, List<SerializableLintError>>) {
        lintErrors.forEach { (filePath, errors) ->
            errors.forEach { error ->
                reportProblem(error, filePath)
            }
        }
    }

    fun reportProblem(error: SerializableLintError, filePath: String) {
        val reporter: ProblemReporter = problems.reporter

        val group = ProblemGroup.create("validation", "ktlint issue")
        val id = ProblemId.create(error.ruleId, error.detail, group)
        reporter.report(id) {
            fileLocation(filePath)
            lineInFileLocation(filePath, error.line)
            details(error.detail)
            severity(Severity.WARNING)
            solution("Run ktlintFormat to auto-fix this issue")
        }
    }
}
