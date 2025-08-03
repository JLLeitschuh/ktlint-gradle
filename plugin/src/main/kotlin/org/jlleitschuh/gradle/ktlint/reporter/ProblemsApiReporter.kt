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
class ProblemsApiReporter {

    private var problems: Problems? = null

    @Inject
    public constructor(problems: Problems) {
        this.problems = problems
    }

    public constructor() {
        this.problems = null
    }

    fun reportProblems(lintErrors: Map<String, List<SerializableLintError>>) {
        val reporter: ProblemReporter? = problems?.reporter

        lintErrors.forEach { (filePath, errors) ->
            errors.forEach { error ->
                val group = ProblemGroup.create("validation", "ktlint issue")
                val id = ProblemId.create(error.ruleId, error.detail, group)
                reporter?.report(id) {
                    fileLocation(filePath)
                    lineInFileLocation(filePath, error.line)
                    details(error.detail)
                    severity(Severity.WARNING)
                    solution("Run ktlintFormat to auto-fix this issue")
                }
            }
        }
    }

    fun reportProblem(error: SerializableLintError, filePath: String) {
        val reporter: ProblemReporter? = problems?.reporter

        val group = ProblemGroup.create("validation", "ktlint issue")
        val id = ProblemId.create(error.ruleId, error.detail, group)
        reporter?.report(id) {
            fileLocation(filePath)
            lineInFileLocation(filePath, error.line)
            details(error.detail)
            severity(Severity.WARNING)
            solution("Run ktlintFormat to auto-fix this issue")
        }
    }
}
