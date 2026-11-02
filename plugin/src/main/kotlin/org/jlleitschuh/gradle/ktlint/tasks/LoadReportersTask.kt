package org.jlleitschuh.gradle.ktlint.tasks

import net.swiftzer.semver.SemVer
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.KTLINT_CONFIGURATION_NAME
import org.jlleitschuh.gradle.ktlint.KTLINT_REPORTER_CONFIGURATION_NAME
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.checkMinimalSupportedKtLintVersion
import org.jlleitschuh.gradle.ktlint.intermediateResultsBuildDir
import org.jlleitschuh.gradle.ktlint.reporter.CustomReporter
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jlleitschuh.gradle.ktlint.worker.LoadReportersWorkAction
import javax.inject.Inject

@CacheableTask
internal abstract class LoadReportersTask @Inject constructor(
    private val workerExecutor: WorkerExecutor
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
    internal abstract val loadedReporters: RegularFileProperty

    @get:OutputFile
    internal abstract val loadedReporterProviders: RegularFileProperty

    @TaskAction
    fun loadReporters() {
        checkMinimalSupportedKtLintVersion(ktLintVersion.get())

        // Classloader isolation is enough here as we just want to use some classes from KtLint classpath
        // to load reporters. No KtLint main object is initialized/used in this case.
        val queue = workerExecutor.classLoaderIsolation {
            classpath.from(ktLintClasspath, reportersClasspath)
        }
        val task = this
        queue.submit(LoadReportersWorkAction::class.java) {
            debug.set(task.debug)
            enabledReporters.set(
                task.enabledReporters
                    .map { reporters ->
                        reporters.filter { it.isAvailable() }
                    }
            )
            customReporters.set(task.customReporters)
            loadedReporters.set(task.loadedReporters)
            loadedReporterProviders.set(task.loadedReporterProviders)
            ktLintVersion.set(task.ktLintVersion)
        }
    }

    private fun ReporterType.isAvailable() =
        SemVer.parse(ktLintVersion.get()) >= availableSinceVersion

    internal companion object {
        internal const val TASK_NAME = "loadKtlintReporters"
        internal const val DESCRIPTION = "Preloads required KtLint reporters."
        fun register(project: Project): TaskProvider<LoadReportersTask> {
            val ktlintExtension = project.extensions.getByType(KtlintExtension::class.java)
            val ktlintConfiguration = project.configurations.named(KTLINT_CONFIGURATION_NAME)
            val ktlintReporterConfiguration = project.configurations.named(KTLINT_REPORTER_CONFIGURATION_NAME)
            return project.tasks.register(TASK_NAME, LoadReportersTask::class.java) {
                description = DESCRIPTION
                ktLintClasspath.setFrom(ktlintConfiguration)
                reportersClasspath.setFrom(ktlintReporterConfiguration)
                debug.set(ktlintExtension.debug)
                ktLintVersion.set(ktlintExtension.version)
                enabledReporters.set(ktlintExtension.reporterExtension.reporters)
                customReporters.set(ktlintExtension.reporterExtension.customReporters)
                loadedReporters.set(project.layout.intermediateResultsBuildDir("reporters.bin"))
                loadedReporterProviders.set(project.layout.intermediateResultsBuildDir("reporterProviders.bin"))
            }
        }
    }
}
