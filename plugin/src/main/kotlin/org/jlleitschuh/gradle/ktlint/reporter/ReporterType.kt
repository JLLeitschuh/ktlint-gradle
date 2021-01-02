package org.jlleitschuh.gradle.ktlint.reporter

import net.swiftzer.semver.SemVer
import java.io.Serializable

enum class ReporterType(
    val reporterName: String,
    val availableSinceVersion: SemVer,
    val fileExtension: String
) : Serializable {
    PLAIN("plain", SemVer(0, 9, 0), "txt"),
    PLAIN_GROUP_BY_FILE("plain?group_by_file", SemVer(0, 9, 0), "txt"),
    CHECKSTYLE("checkstyle", SemVer(0, 9, 0), "xml"),
    JSON("json", SemVer(0, 9, 0), "json"),
    HTML("html", SemVer(0, 36, 0), "html");

    companion object {
        private const val serialVersionUID: Long = 201202199L
    }
}
