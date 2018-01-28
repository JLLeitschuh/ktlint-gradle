// Collection of functions that applies output Ktlint reporter

package org.jlleitschuh.gradle.ktlint.reporter

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.util.GFileUtils
import java.io.File
import net.swiftzer.semver.SemVer
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.ReporterType as DeprecatedReporterType
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Apply reporter to the task.
 */
internal fun JavaExec.applyReporters(target: Project, extension: KtlintExtension, sourceSetName: String) {
    // Multiple reporters are available from ktlint 0.10.0
    if (SemVer.parse(extension.version) >= SemVer(0, 10, 0)) {
        setMultipleReporters(extension, target, sourceSetName)
    } else {
        setOneReporter(extension, target, sourceSetName)
    }
}

private fun JavaExec.setMultipleReporters(
        extension: KtlintExtension,
        target: Project,
        sourceSetName: String
) {
    extension.reporters.forEach { reporter ->
        checkReporterAvailable(reporter, extension, target) {
            applyOutputReporter(reporter, target, sourceSetName)
        }
    }

    val oldReporter = extension.reporter
    if (oldReporter != null) {
        checkReporterAvailable(oldReporter.toNewReporter(), extension, target) {
            applyOutputReporter(oldReporter.toNewReporter(), target, sourceSetName)
        }
    }

    if (extension.outputToConsole) {
        this.args("--reporter=plain")
    }
}

private fun JavaExec.setOneReporter(
        extension: KtlintExtension,
        target: Project,
        sourceSetName: String
) {
    val oldReporter = extension.reporter
    if (oldReporter != null) {
        checkReporterAvailable(oldReporter.toNewReporter(), extension, target) {
            applyOnlyOneOutputReporter(target, oldReporter.toNewReporter(), sourceSetName, extension)
        }
    } else {
        extension.reporters.firstOrNull()?.let { reporter ->
            checkReporterAvailable(reporter, extension, target) {
                applyOnlyOneOutputReporter(target, reporter, sourceSetName, extension)
            }
        }
    }
}

private fun JavaExec.applyOnlyOneOutputReporter(
        target: Project,
        reporter: ReporterType,
        sourceSetName: String,
        extension: KtlintExtension
) {
    var reportOutput: FileOutputStream? = null
    doFirst {
        reportOutput = createReportOutputFile(target, reporter.fileExtension, sourceSetName).outputStream().also {
            this.args("--reporter=${reporter.reporterName}")
            if (extension.outputToConsole) {
                this.standardOutput = object : OutputStream() {
                    override fun write(b: Int) {
                        it.write(b)
                        System.out.write(b)
                    }
                }
            } else {
                this.standardOutput = it
            }
        }
    }
    doLast { reportOutput?.close() }
}

private inline fun checkReporterAvailable(
        reporter: ReporterType,
        extension: KtlintExtension,
        target: Project,
        applyReporter: () -> Unit
) {
    if (SemVer.parse(extension.version) >= reporter.availableSinceVersion) {
        applyReporter()
    } else {
        target.logger.info("Reporter ${reporter.reporterName} is not available in ktlint version ${extension.version}.")
    }
}

private fun JavaExec.applyOutputReporter(reporter: ReporterType, target: Project, sourceSetName: String) {
    doFirst {
        val reporterOutput = createReportOutputFile(target, reporter.fileExtension, sourceSetName)
        this.args("--reporter=${reporter.reporterName},output=${reporterOutput.absolutePath}")
    }
}

private fun createReportOutputFile(target: Project, fileExtension: String, sourceSetName: String): File {
    val reportsDir = File(target.buildDir, "reports/ktlint")
    GFileUtils.mkdirs(reportsDir)
    return File(reportsDir, "ktlint-$sourceSetName.$fileExtension")
}

private fun DeprecatedReporterType.toNewReporter() =
        when (this) {
            DeprecatedReporterType.PLAIN -> ReporterType.PLAIN
            DeprecatedReporterType.PLAIN_GROUP_BY_FILE -> ReporterType.PLAIN_GROUP_BY_FILE
            DeprecatedReporterType.CHECKSTYLE -> ReporterType.CHECKSTYLE
            DeprecatedReporterType.JSON -> ReporterType.JSON
        }
