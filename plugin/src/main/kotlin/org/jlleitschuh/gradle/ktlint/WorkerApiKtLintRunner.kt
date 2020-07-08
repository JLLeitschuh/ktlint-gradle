package org.jlleitschuh.gradle.ktlint

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

abstract class WorkerApiKtLintRunner : KtLintRunner {
    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    override fun lint(ktlintClasspath: FileCollection, ktlintVersion: String, ignoreFailures: Boolean, ktlintArgsFile: File) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

interface KtLintParameters : WorkParameters {
    val ktlintClasspath: ConfigurableFileCollection
    val ktlintVersion: Property<String>
    val ignoreFailures: Property<Boolean>
    val ktlintArgsFile: RegularFileProperty
}

abstract class KtLintWorkAction : WorkAction<KtLintParameters> {
    override fun execute() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}