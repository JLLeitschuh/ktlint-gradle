// Collection of functions that applies output Ktlint reporter

package org.jlleitschuh.gradle.ktlint.reporter

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.util.GFileUtils
import java.io.File
import net.swiftzer.semver.SemVer
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Apply reporter to the task.
 */
internal fun JavaExec.applyReporters(target: Project, extension: KtlintExtension, sourceSetName: String) {
    if (SemVer.parse(extension.version).compareTo(SemVer(0, 10, 0)) >= 0) {
        extension.reporters.forEach { reporter ->
            doFirst {
                val reportOutputFile = createReportOutputFile(target, reporter.fileExtension, sourceSetName)
                this.args("--reporter=${reporter.reporterName},output=${reportOutputFile.absolutePath}")
            }
        }
        if (extension.outputToConsole) {
            this.args("--reporter=plain")
        }
    } else {
        extension.reporters.firstOrNull()?.let { reporter ->
            if (SemVer.parse(extension.version).compareTo(reporter.availableSinceVersion) >= 0) {
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
            } else {
                target.logger.info("Reporter is not available in this ktlint version")
            }
        }
    }
}

private fun createReportOutputFile(target: Project, fileExtension: String, sourceSetName: String): File {
    val reportsDir = File(target.buildDir, "reports/ktlint")
    GFileUtils.mkdirs(reportsDir)
    return File(reportsDir, "ktlint-$sourceSetName.${fileExtension}")
}
