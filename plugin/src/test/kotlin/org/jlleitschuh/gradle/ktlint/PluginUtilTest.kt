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

    @Test
    fun `test readKtlintVersionFromPropertiesFile returns version when file exists`() {
        temporaryFolder.resolve(KTLINT_PLUGINS_PROPERTIES_FILE_NAME).apply {
            createNewFile()
            writeText(
                """
                ktlint-version=1.2.1
                """.trimIndent()
            )
            val version = readKtlintVersionFromPropertiesFile(temporaryFolder.toPath())
            Assertions.assertEquals("1.2.1", version) {
                "correctly reads version from properties file"
            }
            delete()
        }
    }

    @Test
    fun `test readKtlintVersionFromPropertiesFile returns null when file does not exist`() {
        val version = readKtlintVersionFromPropertiesFile(temporaryFolder.toPath())
        Assertions.assertNull(version) {
            "returns null when properties file does not exist"
        }
    }

    @Test
    fun `test readKtlintVersionFromPropertiesFile returns null when version property is missing`() {
        temporaryFolder.resolve(KTLINT_PLUGINS_PROPERTIES_FILE_NAME).apply {
            createNewFile()
            writeText(
                """
                some-other-property=value
                """.trimIndent()
            )
            val version = readKtlintVersionFromPropertiesFile(temporaryFolder.toPath())
            Assertions.assertNull(version) {
                "returns null when ktlint-version property is missing"
            }
            delete()
        }
    }

    @Test
    fun `test readKtlintVersionFromPropertiesFile handles whitespace correctly`() {
        temporaryFolder.resolve(KTLINT_PLUGINS_PROPERTIES_FILE_NAME).apply {
            createNewFile()
            writeText(
                """
                ktlint-version = 1.3.0
                """.trimIndent()
            )
            val version = readKtlintVersionFromPropertiesFile(temporaryFolder.toPath())
            Assertions.assertEquals("1.3.0", version) {
                "correctly handles whitespace around equals sign"
            }
            delete()
        }
    }

    @Test
    fun `test readKtlintVersionFromPropertiesFile returns null when version is blank`() {
        temporaryFolder.resolve(KTLINT_PLUGINS_PROPERTIES_FILE_NAME).apply {
            createNewFile()
            writeText(
                """
                ktlint-version=
                """.trimIndent()
            )
            val version = readKtlintVersionFromPropertiesFile(temporaryFolder.toPath())
            Assertions.assertNull(version) {
                "returns null when ktlint-version is blank"
            }
            delete()
        }
    }

    @Test
    fun `test readKtlintVersionFromPropertiesFile handles multiple properties`() {
        temporaryFolder.resolve(KTLINT_PLUGINS_PROPERTIES_FILE_NAME).apply {
            createNewFile()
            writeText(
                """
                some-property=value1
                ktlint-version=1.4.0
                another-property=value2
                """.trimIndent()
            )
            val version = readKtlintVersionFromPropertiesFile(temporaryFolder.toPath())
            Assertions.assertEquals("1.4.0", version) {
                "correctly finds ktlint-version among multiple properties"
            }
            delete()
        }
    }
}
