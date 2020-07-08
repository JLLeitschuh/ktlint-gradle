package org.jlleitschuh.gradle.ktlint

import java.io.File
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ProcessOperations

class JavaExecKtLintRunner(private val processOperations: ProcessOperations) : KtLintRunner {
    override fun lint(ktlintClasspath: FileCollection, ktlintVersion: String, ignoreFailures: Boolean, ktlintArgsFile: File) {
        processOperations.javaexec {
            it.classpath = ktlintClasspath
            it.main = resolveMainClassName(ktlintVersion)
            it.isIgnoreExitValue = ignoreFailures
            it.args("@$ktlintArgsFile")
        }
    }
}
