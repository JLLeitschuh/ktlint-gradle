package org.jlleitschuh.gradle.ktlint

import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.junit.jupiter.api.io.TempDir
import java.io.File

@GradleTestVersions
abstract class AbstractPluginTest {

    @TempDir
    lateinit var temporaryFolder: File

    val projectRoot: File
        get() = temporaryFolder.resolve("plugin-test").apply { mkdirs() }

    val mainSourceSetCheckTaskName = GenerateReportsTask.generateNameForSourceSets(
        "main",
        GenerateReportsTask.LintType.CHECK
    )

    val mainSourceSetFormatTaskName = GenerateReportsTask.generateNameForSourceSets(
        "main",
        GenerateReportsTask.LintType.FORMAT
    )

    val kotlinScriptCheckTaskName = GenerateReportsTask.generateNameForKotlinScripts(
        GenerateReportsTask.LintType.CHECK
    )

    protected fun File.withCleanSources() = createSourceFile(
        "src/main/kotlin/CleanSource.kt",
        """
            val foo = "bar"

        """.trimIndent()
    )

    protected fun File.withCleanKotlinScript() = createSourceFile(
        "kotlin-script.kts",
        """
            println("zzz")

        """.trimIndent()
    )

    protected fun File.withFailingKotlinScript() = createSourceFile(
        "kotlin-script-fail.kts",
        """
            println("zzz")

        """.trimIndent()
    )

    protected fun File.withAlternativeFailingSources(baseDir: String) =
        createSourceFile("$baseDir/FailSource.kt", """val  foo    =     "bar"""")

    protected fun File.createSourceFile(sourceFilePath: String, contents: String) {
        val sourceFile = resolve(sourceFilePath)
        sourceFile.parentFile.mkdirs()
        sourceFile.writeText(contents)
    }
}
