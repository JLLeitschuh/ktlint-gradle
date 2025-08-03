package org.jlleitschuh.gradle.ktlint.reporter

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Action
import org.gradle.api.problems.ProblemReporter
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity as GradleSeverity
import org.jlleitschuh.gradle.ktlint.worker.SerializableLintError
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProblemsApiReporterTest {
    private lateinit var problemsService: Problems
    private lateinit var problemReporter: ProblemReporter
    private lateinit var reporter: ProblemsApiReporter

    @BeforeEach
    fun setUp() {
        problemsService = mock()
        problemReporter = mock()
        reporter = ProblemsApiReporter(problemsService)
        whenever(problemsService.reporter).thenReturn(problemReporter)
    }

    @Test
    fun `given a lint error, it correctly reports it to the Gradle Problems API`() {
        val error = SerializableLintError(
            line = 4,
            col = 1,
            ruleId = "no-missing-newline",
            detail = "Missing newline at end of file",
            canBeAutoCorrected = true
        )
        val filePath = "src/main/kotlin/TestFile.kt"

        reporter.reportProblem(error, filePath)

        val specCaptor = argumentCaptor<Action<ProblemSpec>>()
        verify(problemReporter).report(any(), specCaptor.capture())

        val spec: ProblemSpec = mock()
        specCaptor.firstValue.execute(spec)

        verify(spec).details("Missing newline at end of file")
        verify(spec).severity(GradleSeverity.WARNING)
        verify(spec).lineInFileLocation(eq(filePath), eq(4))
        verify(spec).fileLocation(eq(filePath))
        verify(spec).solution("Run ktlintFormat to auto-fix this issue")
    }

    @Test
    fun `given multiple lint errors, it correctly reports all of them to the problems api`() {
        val errors = mapOf(
            "src/main/kotlin/File1.kt" to listOf(
                SerializableLintError(
                    line = 100,
                    col = 1,
                    ruleId = "no-missing-newline",
                    detail = "Missing newline at end of file",
                    canBeAutoCorrected = true
                )
            ),
            "src/main/kotlin/File2.kt" to listOf(
                SerializableLintError(
                    line = 5,
                    col = 10,
                    ruleId = "no-unused-imports",
                    detail = "Unused import",
                    canBeAutoCorrected = false
                ),
                SerializableLintError(
                    line = 10,
                    col = 5,
                    ruleId = "no-trailing-whitespace",
                    detail = "Trailing whitespace",
                    canBeAutoCorrected = true
                )
            )
        )

        reporter.reportProblems(errors)

        verify(problemReporter, org.mockito.kotlin.times(3)).report(any(), any())
    }

    @Test
    fun `no problems should be reported if there are no errors`() {
        val emptyErrors = emptyMap<String, List<SerializableLintError>>()

        reporter.reportProblems(emptyErrors)

        verify(problemReporter, never()).report(any(), any())
    }

    @Test
    fun `the problems service is not available when reporting`() {
        val reporterWithoutProblems = ProblemsApiReporter()
        val error = SerializableLintError(
            line = 1,
            col = 1,
            ruleId = "test-rule",
            detail = "Test error",
            canBeAutoCorrected = false
        )

        reporterWithoutProblems.reportProblem(error, "test.kt")

        verify(problemReporter, never()).report(any(), any())
    }

    @Test
    fun `correctly reports problem group and id`() {
        val error = SerializableLintError(
            line = 1,
            col = 1,
            ruleId = "no-unused-imports",
            detail = "Unused import",
            canBeAutoCorrected = true
        )
        val filePath = "src/main/kotlin/TestFile.kt"

        reporter.reportProblem(error, filePath)

        val idCaptor = argumentCaptor<org.gradle.api.problems.ProblemId>()
        verify(problemReporter).report(idCaptor.capture(), any())

        val capturedId = idCaptor.firstValue
        assertThat(capturedId.name).isEqualTo("no-unused-imports")
        assertThat(capturedId.displayName).isEqualTo("Unused import")
        assertThat(capturedId.group.name).isEqualTo("validation")
        assertThat(capturedId.group.displayName).isEqualTo("ktlint issue")
    }

    @Test
    fun `handles different error types correctly`() {
        val autoCorrectableError = SerializableLintError(
            line = 1,
            col = 1,
            ruleId = "auto-correctable",
            detail = "Can be fixed",
            canBeAutoCorrected = true
        )
        val nonAutoCorrectableError = SerializableLintError(
            line = 2,
            col = 1,
            ruleId = "manual-fix",
            detail = "Must fix manually",
            canBeAutoCorrected = false
        )

        reporter.reportProblem(autoCorrectableError, "test.kt")
        reporter.reportProblem(nonAutoCorrectableError, "test.kt")

        verify(problemReporter, org.mockito.kotlin.times(2)).report(any(), any())
    }
}
