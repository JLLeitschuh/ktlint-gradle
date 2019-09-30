package org.jlleitschuh.gradle.ktlint.reporter

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile

internal sealed class KtlintReport {
    abstract val reporterId: String
    abstract val outputFile: RegularFileProperty
    abstract fun asArgument(): String

    data class BuiltIn(
        @get:Input
        override val reporterId: String,
        @get:Input
        val enabled: Property<Boolean>,
        @get:Input
        val reporterType: ReporterType,
        @get:OutputFile
        override val outputFile: RegularFileProperty
    ) : KtlintReport() {

        override fun asArgument(): String =
            "--reporter=${reporterType.reporterName},output=${outputFile.get().asFile.absolutePath}"
    }

    class CustomReport(
        @get:Input
        override val reporterId: String,
        @get:InputFile
        val customReporterJar: RegularFileProperty,
        @get:OutputFile
        override val outputFile: RegularFileProperty
    ) : KtlintReport() {
        override fun asArgument(): String =
            "--reporter=$reporterId" +
                ",artifact=${customReporterJar.get().asFile.absolutePath}" +
                ",output=${outputFile.get().asFile.absolutePath}"
    }
}
