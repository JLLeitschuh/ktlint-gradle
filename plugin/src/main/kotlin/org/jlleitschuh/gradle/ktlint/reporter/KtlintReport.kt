package org.jlleitschuh.gradle.ktlint.reporter

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile

class KtlintReport(
    @get: Input
    val enabled: Property<Boolean>,
    @get: Input
    val reporterType: ReporterType,
    @get: OutputFile
    val outputFile: RegularFileProperty
) {
    fun asArgument() = "--reporter=${reporterType.reporterName},output=${outputFile.get().asFile.absolutePath}"
}