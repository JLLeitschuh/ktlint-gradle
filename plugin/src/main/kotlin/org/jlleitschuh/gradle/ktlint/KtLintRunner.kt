package org.jlleitschuh.gradle.ktlint

import java.io.File
import org.gradle.api.file.FileCollection

interface KtLintRunner {
    fun lint(ktlintClasspath: FileCollection, ktlintVersion: String, ignoreFailures: Boolean, ktlintArgsFile: File)
}
