package org.jlleitschuh.gradle.ktlint.tasks

import net.swiftzer.semver.SemVer
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.intermediateResultsBuildDir
import org.jlleitschuh.gradle.ktlint.reporter.CustomReporter
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jlleitschuh.gradle.ktlint.worker.LoadReportersWorkAction
import javax.inject.Inject

@Suppress("UnstableApiUsage")
internal abstract class LoadReportersTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
) : DefaultTask() {

    @get:Classpath
    internal abstract val ktLintClasspath: ConfigurableFileCollection

    @get:Classpath
    internal abstract val reportersClasspath: ConfigurableFileCollection

    @get:Input
    internal abstract val debug: Property<Boolean>

    @get:Input
    internal abstract val ktLintVersion: Property<String>

    @get:Input
    internal abstract val enabledReporters: SetProperty<ReporterType>

    @get:Input
    internal abstract val customReporters: SetProperty<CustomReporter>

    @get:OutputFile
    internal val loadedReporters: RegularFileProperty = objectFactory.fileProperty().convention(
        projectLayout.intermediateResultsBuildDir("reporters.bin")
    )

    @TaskAction
    fun loadReporters() {
        val queue = workerExecutor.classLoaderIsolation { spec ->
            spec.classpath.setFrom(ktLintClasspath, reportersClasspath)
        }

        queue.submit(LoadReportersWorkAction::class.java) { param ->
            param.debug.set(debug)
            param.enabledReporters.set(
                enabledReporters
                    .map { reporters ->
                        reporters.filter { it.isAvailable() }
                    }
            )
            param.customReporters.set(customReporters)
            param.loadedReporters.set(loadedReporters)
        }
    }

    private fun ReporterType.isAvailable() =
        SemVer.parse(ktLintVersion.get()) >= availableSinceVersion

    companion object {
        internal const val TASK_NAME = "loadKtLintReporters"
        internal const val DESCRIPTION = "Preloads required KtLint reporters."
    }
}
