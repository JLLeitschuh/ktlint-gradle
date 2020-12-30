package org.jlleitschuh.gradle.ktlint.worker

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jlleitschuh.gradle.ktlint.resolveMainClassName
import javax.inject.Inject

@Suppress("UnstableApiUsage")
abstract class KtLintWorkAction : WorkAction<KtLintParameters> {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun execute() {
        execOperations.javaexec {
            it.classpath = parameters.ktlintClasspath
            it.main = resolveMainClassName(parameters.ktlintVersion.get())
            it.isIgnoreExitValue = parameters.ignoreFailures.get()
            it.args("@${parameters.ktlintArgsFile.asFile.get()}")
        }
    }
}

@Suppress("UnstableApiUsage")
interface KtLintParameters : WorkParameters {
    val ktlintClasspath: ConfigurableFileCollection
    val ktlintVersion: Property<String>
    val ignoreFailures: Property<Boolean>
    val ktlintArgsFile: RegularFileProperty
}
