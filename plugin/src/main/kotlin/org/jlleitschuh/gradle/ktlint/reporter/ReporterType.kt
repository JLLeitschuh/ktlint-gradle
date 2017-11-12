package org.jlleitschuh.gradle.ktlint.reporter

import net.swiftzer.semver.SemVer

enum class ReporterType(val reporterName: String, val availableSinceVersion: SemVer, val fileExtension: String) {
    PLAIN("plain", SemVer(0, 9, 0), "txt"),
    PLAIN_GROUP_BY_FILE("plain?group_by_file", SemVer(0, 9, 0), "txt"),
    CHECKSTYLE("checkstyle", SemVer(0, 9, 0), "xml"),
    JSON("json", SemVer(0, 9, 0), "json");
}