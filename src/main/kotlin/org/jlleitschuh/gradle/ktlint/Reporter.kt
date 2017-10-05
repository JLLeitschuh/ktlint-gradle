// Collection of functions that applies output Ktlint reporter

package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.util.GFileUtils
import java.io.File

/**
 * Supported reporter types.
 */
enum class ReporterType(val reporterName: String, val availableSinceVersion: String, val fileExtension: String) {
    PLAIN("plain", "0.9.0", "txt"),
    PLAIN_GROUP_BY_FILE("plain?group_by_file", "0.9.0", "txt"),
    CHECKSTYLE("checkstyle", "0.9.0", "xml"),
    JSON("json", "0.9.0", "json");
}

/**
 * Apply reporter to the task.
 */
fun JavaExec.applyReporter(target: Project, extension: KtlintExtension) {
    if (isReportAvailable(extension.version, extension.reporter.availableSinceVersion)) {
        val reportOutput = createReportOutputDir(target, extension).outputStream()
        this.args("--reporter=${extension.reporter.reporterName}")
        this.standardOutput = reportOutput
        doLast { reportOutput.close() }
    } else {
        target.logger.info("Reporter is not available in this ktlint version")
    }
}

private fun isReportAvailable(version: String, availableSinceVersion: String): Boolean {
    val versionsNumbers = version.split('.')
    val reporterVersionNumbers = availableSinceVersion.split('.')
    return versionsNumbers[0] >= reporterVersionNumbers[0] &&
            versionsNumbers[1] >= reporterVersionNumbers[1] &&
            versionsNumbers[2] >= reporterVersionNumbers[2]
}

private fun createReportOutputDir(target: Project, extension: KtlintExtension): File {
    val reportsDir = File(target.buildDir, "reports/ktlint")
    GFileUtils.mkdirs(reportsDir)
    return File(reportsDir, "ktlint.${extension.reporter.fileExtension}")
}
