package org.jlleitschuh.gradle.ktlint.reporter

import net.swiftzer.semver.SemVer

enum class ReporterType(val reporterName: String, val availableSinceVersion: SemVer, val fileExtension: String) {
    plain("plain", SemVer(0, 9, 0), "txt"),
    plain_group_by_file("plain?group_by_file", SemVer(0, 9, 0), "txt"),
    checkstyle("checkstyle", SemVer(0, 9, 0), "xml"),
    json("json", SemVer(0, 9, 0), "json");
}