package org.jlleitschuh.gradle.ktlint

import org.gradle.api.file.FileCollection
import java.io.File

interface KtLintRunner {
    fun lint(
        ktlintClasspath: FileCollection,
        ktlintVersion: String,
        ignoreFailures: Boolean,
        ktlintArgsFile: File
    )
}
