package org.jlleitschuh.gradle.ktlint.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.worker.GenerateBaselineWorkAction
import javax.inject.Inject

/**
 * Generates KtLint baseline file.
 *
 * If baseline file is already exists - it will be overwritten.
 */
@CacheableTask
abstract class GenerateBaselineTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    private val projectLayout: ProjectLayout
) : DefaultTask() {
    @get:Classpath
    internal abstract val ktLintClasspath: ConfigurableFileCollection

    @get:Classpath
    internal abstract val baselineReporterClasspath: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal abstract val discoveredErrors: ConfigurableFileCollection

    @get:Input
    internal abstract val ktLintVersion: Property<String>

    @get:OutputFile
    abstract val baselineFile: RegularFileProperty

    final override fun onlyIf(spec: Spec<in Task>) {
        super.onlyIf(spec)
    }

    @Suppress("UnstableApiUsage")
    @TaskAction
    fun generateBaseline() {
        // Classloader isolation is enough here as we just want to use some classes from KtLint classpath
        // to get errors and generate files/console reports. No KtLint main object is initialized/used in this case.
        val queue = workerExecutor.classLoaderIsolation { spec ->
            spec.classpath.from(ktLintClasspath, baselineReporterClasspath)
        }

        queue.submit(GenerateBaselineWorkAction::class.java) { param ->
            param.discoveredErrors.setFrom(discoveredErrors)
            param.ktLintVersion.set(ktLintVersion)
            param.baselineFile.set(baselineFile)
            param.projectDirectory.set(projectLayout.projectDirectory)
        }
    }

    companion object {
        const val NAME = "ktlintGenerateBaseline"
        const val DESCRIPTION = "Generates KtLint baseline file"
    }
}
