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
            it.classpath.from(ktLintClasspath, reportersClasspath)
        }

        queue.submit(LoadReportersWorkAction::class.java) {
            val task = this@LoadReportersTask
            it.debug.set(task.debug)
            it.enabledReporters.set(
                task.enabledReporters
                    .map { reporters ->
                        reporters.filter { it.isAvailable() }
                    }
            )
            it.customReporters.set(task.customReporters)
            it.loadedReporters.set(task.loadedReporters)
            it.loadedReporterProviders.set(task.loadedReporterProviders)
            it.ktLintVersion.set(task.ktLintVersion)
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
                it.description = DESCRIPTION
                it.ktLintClasspath.setFrom(ktlintConfiguration)
                it.reportersClasspath.setFrom(ktlintReporterConfiguration)
                it.debug.set(ktlintExtension.debug)
                it.ktLintVersion.set(ktlintExtension.version)
                it.enabledReporters.set(ktlintExtension.reporterExtension.reporters)
                it.customReporters.set(ktlintExtension.reporterExtension.customReporters)
                it.loadedReporters.set(project.layout.intermediateResultsBuildDir("reporters.bin"))
                it.loadedReporterProviders.set(project.layout.intermediateResultsBuildDir("reporterProviders.bin"))
            }
        }
    }
}
