package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.Reporter
import com.pinterest.ktlint.core.ReporterProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.PrintStream

internal class SerializableReporterProviderTest {
    @TempDir
    lateinit var temporaryFolder: File

    @Test
    internal fun `Should correctly serialize and deserialize ReporterProvider`() {
        val reporterProvider = TestReporterProvider()
        val wrappedReporterProvider = SerializableReporterProvider(reporterProvider)
        val serializeIntoFile = temporaryFolder.resolve("reporters.test")

        ObjectOutputStream(serializeIntoFile.outputStream()).use {
            it.writeObject(wrappedReporterProvider)
        }

        ObjectInputStream(serializeIntoFile.inputStream()).use {
            val restoredWrappedReporterProvider = it.readObject() as SerializableReporterProvider
            assertThat(restoredWrappedReporterProvider.reporterProvider).isInstanceOf(TestReporterProvider::class.java)
            assertThat(restoredWrappedReporterProvider.reporterProvider.id)
                .isEqualTo(reporterProvider.id)
        }
    }

    private class TestReporterProvider : ReporterProvider<Reporter> {
        override val id: String = "test-reporter-provider"

        override fun get(
            out: PrintStream,
            opt: Map<String, String>
        ): Reporter {
            TODO("Not yet implemented")
        }
    }
}
