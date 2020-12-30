package org.jlleitschuh.gradle.ktlint

import org.gradle.api.file.FileCollection
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.worker.KtLintWorkAction
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
