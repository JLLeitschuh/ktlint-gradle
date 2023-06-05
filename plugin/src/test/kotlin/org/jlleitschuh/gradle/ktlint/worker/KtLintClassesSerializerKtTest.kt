package org.jlleitschuh.gradle.ktlint.worker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.ObjectOutputStream

class KtLintClassesSerializerKtTest {
    @TempDir
    lateinit var temporaryFolder: File

    @Test
    internal fun `loadAnyErrors can read files written by our error`() {
        val serializableLintError = SerializableLintError(14, 154, "test-rule", "details about error", false)
        val file = temporaryFolder.resolve("lintError.test")

        val input = ArrayList<LintErrorResult>().apply {
            add(
                LintErrorResult(
                    lintedFile = file,
                    lintErrors = ArrayList<Pair<SerializableLintError, Boolean>>()
                        .apply { add(serializableLintError to true) }
                )
            )
        }
        ObjectOutputStream(file.outputStream()).use {
            it.writeObject(input)
        }
        val actual = loadAnyErrors(file)
        assertThat(actual).isEqualTo(input)
    }
}
