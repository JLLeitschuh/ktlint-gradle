package org.jlleitschuh.gradle.ktlint.reporter

import net.swiftzer.semver.SemVer
import java.io.Serializable

enum class ReporterType(
    val reporterName: String,
    val availableSinceVersion: SemVer,
    val fileExtension: String,
    val options: List<String>,
) : Serializable {
    PLAIN("plain", SemVer(0, 9, 0), "txt", emptyList()),
    PLAIN_GROUP_BY_FILE("plain", SemVer(0, 9, 0), "txt", listOf("group_by_file")),
    CHECKSTYLE("checkstyle", SemVer(0, 9, 0), "xml", emptyList()),
    JSON("json", SemVer(0, 9, 0), "json", emptyList()),
    SARIF("sarif", SemVer(0, 42, 0), "sarif", emptyList()),
    HTML("html", SemVer(0, 36, 0), "html", emptyList());

    companion object {
        private const val serialVersionUID: Long = 201202199L
    }
}
