package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.LintError
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

internal class SerializableLintErrorTest {
    @TempDir
    lateinit var temporaryFolder: File

    @Test
    internal fun `Should correctly serialize and deserialize LintError`() {
        val lintError = LintError(
            14, 154, "test-rule", "details about error", false
        )
        val wrappedLintError = SerializableLintError(lintError)
        val serializeIntoFile = temporaryFolder.resolve("lintError.test")

        ObjectOutputStream(serializeIntoFile.outputStream()).use {
            it.writeObject(wrappedLintError)
        }

        ObjectInputStream(serializeIntoFile.inputStream()).use {
            val restoredWrappedLintError = it.readObject() as SerializableLintError
            assertThat(restoredWrappedLintError.lintError).isEqualTo(lintError)
            assertThat(restoredWrappedLintError.lintError.canBeAutoCorrected)
                .isEqualTo(lintError.canBeAutoCorrected)
        }
    }
}
