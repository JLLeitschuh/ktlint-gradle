// Collection of functions that applies output Ktlint reporter

package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.util.GFileUtils
import java.io.File
import java.io.FileOutputStream
import net.swiftzer.semver.SemVer

/**
 * Supported reporter types.
 */
enum class ReporterType(val reporterName: String, val availableSinceVersion: SemVer, val fileExtension: String) {
    PLAIN("plain", SemVer(0, 9, 0), "txt"),
    PLAIN_GROUP_BY_FILE("plain?group_by_file", SemVer(0, 9, 0), "txt"),
    CHECKSTYLE("checkstyle", SemVer(0, 9, 0), "xml"),
    JSON("json", SemVer(0, 9, 0), "json");
}

/**
 * Apply reporter to the task.
 */
fun JavaExec.applyReporter(target: Project, extension: KtlintExtension, sourceSetName: String) {
    if (SemVer.parse(extension.version).compareTo(extension.reporter.availableSinceVersion) >= 0) {
        var reportOutput: FileOutputStream? = null
        doFirst {
            reportOutput = createReportOutputDir(target, extension, sourceSetName).outputStream().also {
                this.args("--reporter=${extension.reporter.reporterName}")
                this.standardOutput = it
            }
        }
        doLast { reportOutput?.close() }
    } else {
        target.logger.info("Reporter is not available in this ktlint version")
    }
}

private fun createReportOutputDir(target: Project, extension: KtlintExtension, sourceSetName: String): File {
    val reportsDir = File(target.buildDir, "reports/ktlint")
    GFileUtils.mkdirs(reportsDir)
    return File(reportsDir, "ktlint-$sourceSetName.${extension.reporter.fileExtension}")
}
