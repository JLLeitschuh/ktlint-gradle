package org.jlleitschuh.gradle.ktlint.reporter

import org.gradle.api.artifacts.Dependency

data class CustomReporter(
    val reporterId: String,
    val reporterFileExtension: String,
    val dependency: Dependency
)
