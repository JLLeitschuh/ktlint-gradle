package org.jlleitschuh.gradle.ktlint.reporter

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Action
import org.gradle.api.problems.ProblemReporter
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.jlleitschuh.gradle.ktlint.worker.SerializableLintError
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.gradle.api.problems.Severity as GradleSeverity

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
            ruleId = "no-wildcard-imports",
            detail = "Wildcard import detected",
            canBeAutoCorrected = true
        )
        val filePath = "src/main/kotlin/TestFile.kt"

        reporter.reportProblem(error, filePath, GradleSeverity.ERROR)

        val specCaptor = argumentCaptor<Action<ProblemSpec>>()
        verify(problemReporter).report(any(), specCaptor.capture())

        val spec: ProblemSpec = mock()
        specCaptor.firstValue.execute(spec)

        verify(spec).details("Wildcard import detected")
        verify(spec).severity(GradleSeverity.ERROR)
        verify(spec).lineInFileLocation(eq(filePath), eq(4), eq(1))
        verify(spec).solution("Run ktlintFormat to auto-fix this issue")
    }

    @Test
    fun `given multiple lint errors, it correctly reports all of them`() {
        val errors = mapOf(
            "src/main/kotlin/File1.kt" to listOf(
                SerializableLintError(
                    line = 1,
                    col = 1,
                    ruleId = "no-wildcard-imports",
                    detail = "Wildcard import detected",
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

        reporter.reportProblems(errors, false) // ignoreFailures = false, so ERROR severity

        // Should report 3 total errors
        verify(problemReporter, times(3)).report(any(), any())
    }

    @Test
    fun `given no errors, it does not report any problems`() {
        val emptyErrors = emptyMap<String, List<SerializableLintError>>()

        reporter.reportProblems(emptyErrors, false)

        verify(problemReporter, never()).report(any(), any())
    }

    @Test
    fun `severity is WARNING when ignoreFailures is true`() {
        val errors = mapOf(
            "src/main/kotlin/TestFile.kt" to listOf(
                SerializableLintError(
                    line = 1,
                    col = 1,
                    ruleId = "test-rule",
                    detail = "Test error",
                    canBeAutoCorrected = false
                )
            )
        )

        reporter.reportProblems(errors, true) // ignoreFailures = true, so WARNING severity

        val specCaptor = argumentCaptor<Action<ProblemSpec>>()
        verify(problemReporter).report(any(), specCaptor.capture())

        val spec: ProblemSpec = mock()
        specCaptor.firstValue.execute(spec)

        verify(spec).severity(GradleSeverity.WARNING)
    }

    @Test
    fun `reports correct problem group and id`() {
        val error = SerializableLintError(
            line = 1,
            col = 1,
            ruleId = "no-wildcard-imports",
            detail = "Wildcard import detected",
            canBeAutoCorrected = true
        )
        val filePath = "src/main/kotlin/TestFile.kt"

        reporter.reportProblem(error, filePath, GradleSeverity.ERROR)

        val idCaptor = argumentCaptor<org.gradle.api.problems.ProblemId>()
        verify(problemReporter).report(idCaptor.capture(), any())

        val capturedId = idCaptor.firstValue
        assertThat(capturedId.name).isEqualTo("no-wildcard-imports")
        assertThat(capturedId.displayName).isEqualTo("Wildcard import detected")
        assertThat(capturedId.group.name).isEqualTo("ktlint-gradle")
        assertThat(capturedId.group.displayName).isEqualTo("ktlint-gradle issue")
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

        reporter.reportProblem(autoCorrectableError, "test.kt", GradleSeverity.ERROR)
        reporter.reportProblem(nonAutoCorrectableError, "test.kt", GradleSeverity.ERROR)

        // Both should be reported with ERROR severity
        verify(problemReporter, times(2)).report(any(), any())
    }

    @Test
    fun `reports errors with correct line and column information`() {
        val error = SerializableLintError(
            line = 42,
            col = 15,
            ruleId = "test-rule",
            detail = "Test error",
            canBeAutoCorrected = false
        )
        val filePath = "src/main/kotlin/ComplexFile.kt"

        reporter.reportProblem(error, filePath, GradleSeverity.ERROR)

        val specCaptor = argumentCaptor<Action<ProblemSpec>>()
        verify(problemReporter).report(any(), specCaptor.capture())

        val spec: ProblemSpec = mock()
        specCaptor.firstValue.execute(spec)

        verify(spec).lineInFileLocation(eq(filePath), eq(42), eq(15))
    }
}
