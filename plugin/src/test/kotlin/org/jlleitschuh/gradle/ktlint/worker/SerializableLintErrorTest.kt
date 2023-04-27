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
        val serializableLintError = SerializableLintError(14, 154, "test-rule", "details about error", false)
        val lintError = LintError(14, 154, "test-rule", "details about error", false)
        val coreFile = temporaryFolder.resolve("lintError-core.test")
        val ourFile = temporaryFolder.resolve("lintError-serializable.test")

        ObjectOutputStream(coreFile.outputStream()).use {
            it.writeObject(lintError)
        }

        ObjectOutputStream(ourFile.outputStream()).use {
            it.writeObject(serializableLintError)
        }

        ObjectInputStream(coreFile.inputStream()).use {
            val restoredLintError = it.readObject() as LintError
            assertThat(restoredLintError).isEqualTo(lintError)
            assertThat(restoredLintError.canBeAutoCorrected)
                .isEqualTo(lintError.canBeAutoCorrected)
        }

        ObjectInputStream(ourFile.inputStream()).use {
            val restoredLintError = it.readObject() as SerializableLintError
            assertThat(restoredLintError).isEqualTo(serializableLintError)
            assertThat(restoredLintError.canBeAutoCorrected)
                .isEqualTo(lintError.canBeAutoCorrected)
        }
    }
}
