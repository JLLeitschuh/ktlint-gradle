package org.jlleitschuh.gradle.ktlint

import java.io.File
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

open class JavaExecKtLintRunner @Inject constructor(private val project: Project) : KtLintRunner {
    override fun lint(ktlintClasspath: FileCollection, ktlintVersion: String, ignoreFailures: Boolean, ktlintArgsFile: File) {
        project.javaexec {
            it.classpath = ktlintClasspath
            it.main = resolveMainClassName(ktlintVersion)
            it.isIgnoreExitValue = ignoreFailures
            it.args("@$ktlintArgsFile")
        }
    }
}
