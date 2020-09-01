package org.jlleitschuh.gradle.ktlint

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

abstract class WorkerApiKtLintRunner : KtLintRunner {
    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    override fun lint(ktlintClasspath: FileCollection, ktlintVersion: String, ignoreFailures: Boolean, ktlintArgsFile: File) {
        // TODO: Possibly this could use processIsolation and programmatically execute ktlint
        val queue = workerExecutor.noIsolation()

        queue.submit(KtLintWorkAction::class.java) { params ->
            params.ktlintClasspath.from(ktlintClasspath)
            params.ktlintVersion.set(ktlintVersion)
            params.ignoreFailures.set(ignoreFailures)
            params.ktlintArgsFile.set(ktlintArgsFile)
        }
    }
}

interface KtLintParameters : WorkParameters {
    val ktlintClasspath: ConfigurableFileCollection
    val ktlintVersion: Property<String>
    val ignoreFailures: Property<Boolean>
    val ktlintArgsFile: RegularFileProperty
}

abstract class KtLintWorkAction : WorkAction<KtLintParameters> {
    @get:Inject abstract val execOperations: ExecOperations

    override fun execute() {
        execOperations.javaexec {
            it.classpath = parameters.ktlintClasspath
            it.main = resolveMainClassName(parameters.ktlintVersion.get())
            it.isIgnoreExitValue = parameters.ignoreFailures.get()
            it.args("@${parameters.ktlintArgsFile.asFile.get()}")
        }
    }
}
