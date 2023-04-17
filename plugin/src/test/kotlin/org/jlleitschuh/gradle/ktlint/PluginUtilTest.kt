package org.jlleitschuh.gradle.ktlint

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class PluginUtilTest {
    @TempDir
    lateinit var temporaryFolder: File

    @Test
    fun `test isRootEditorConfig returns true`() {
        temporaryFolder.resolve("test-pos.txt").apply {
            createNewFile()
            writeText(
                """
                root=true
                """.trimIndent()
            )
            Assertions.assertTrue(this.toPath().isRootEditorConfig()) {
                "correctly detects root"
            }
            delete()
        }
    }

    @Test
    fun `test isRootEditorConfig returns false`() {
        temporaryFolder.resolve("test-neg.txt").apply {
            createNewFile()
            writeText(
                """
                [*.kt]
                """.trimIndent()
            )
            Assertions.assertFalse(this.toPath().isRootEditorConfig()) {
                "correctly does not match non-root file"
            }
            delete()
        }
    }
}
