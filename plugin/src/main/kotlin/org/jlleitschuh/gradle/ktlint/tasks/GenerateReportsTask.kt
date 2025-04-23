package org.jlleitschuh.gradle.ktlint.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.reporter.LoadedReporter
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jlleitschuh.gradle.ktlint.worker.ConsoleReportWorkAction
import org.jlleitschuh.gradle.ktlint.worker.GenerateReportsWorkAction
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream
import javax.inject.Inject

/**
 * Generates reports and prints errors into Gradle console.
 *
 * This will actually fail the build in case some non-corrected lint issues.
 */
@CacheableTask
abstract class GenerateReportsTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    private val projectLayout: ProjectLayout,
    objectFactory: ObjectFactory
) : DefaultTask() {
    @get:Classpath
    internal abstract val ktLintClasspath: ConfigurableFileCollection

    @get:Classpath
    internal abstract val reportersClasspath: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    internal abstract val loadedReporterProviders: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    internal abstract val loadedReporters: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    internal abstract val discoveredErrors: RegularFileProperty

    @get:Input
    internal abstract val reportsName: Property<String>

    @get:Input
    internal abstract val enabledReporters: SetProperty<ReporterType>

    @get:Input
    internal abstract val outputToConsole: Property<Boolean>

    @get:Input
    internal abstract val coloredOutput: Property<Boolean>

    @get:Input
    internal abstract val outputColorName: Property<String>

    @get:Input
    internal abstract val ignoreFailures: Property<Boolean>

    @get:Input
    internal abstract val verbose: Property<Boolean>

    @get:Input
    internal abstract val ktLintVersion: Property<String>

    @get:Input
    internal abstract val relative: Property<Boolean>

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    @get:Optional
    internal abstract val baseline: RegularFileProperty

    /**
     * Reading a project's rootDir within a task's action is not allowed for configuration
     * cache, so read it eagerly on task initialization.
     * see: https://docs.gradle.org/current/userguide/configuration_cache.html#config_cache
     */
    private val rootDir: File = project.rootDir

    init {
        // Workaround for https://github.com/gradle/gradle/issues/2919
        onlyIf {
            val errorsFile = (it as GenerateReportsTask).discoveredErrors.asFile.get()
            errorsFile.exists()
        }
    }

    /**
     * Reports output directory.
     *
     * Default is "build/reports/ktlint/${taskName}/".
     */
    @Suppress("UnstableApiUsage")
    @get:OutputDirectory
    val reportsOutputDirectory: DirectoryProperty = objectFactory
        .directoryProperty()
        .convention(
            reportsName.flatMap {
                projectLayout
                    .buildDirectory
                    .dir("reports${File.separator}ktlint${File.separator}$it")
            }
        )

    @Suppress("UnstableApiUsage")
    @TaskAction
    fun generateReports() {
        // Classloader isolation is enough here as we just want to use some classes from KtLint classpath
        // to get errors and generate files/console reports. No KtLint main object is initialized/used in this case.
        val queue = workerExecutor.classLoaderIsolation {
            classpath.from(ktLintClasspath, reportersClasspath)
        }

        val loadedReporters = loadLoadedReporters()
            .associateWith {
                reportsOutputDirectory.file("${reportsName.get()}.${it.fileExtension}")
            }

        val task = this
        loadedReporters.forEach { (loadedReporter, reporterOutput) ->
            queue.submit(GenerateReportsWorkAction::class.java) {
                discoveredErrorsFile.set(task.discoveredErrors)
                loadedReporterProviders.set(task.loadedReporterProviders)
                reporterId.set(loadedReporter.reporterId)
                this.reporterOutput.set(reporterOutput)
                reporterOptions.set(generateReporterOptions(loadedReporter))
                ktLintVersion.set(task.ktLintVersion)
                baseline.set(task.baseline)
                projectDirectory.set(task.projectLayout.projectDirectory)
                if (task.relative.get()) {
                    filePathsRelativeTo.set(rootDir)
                }
            }
        }

        queue.submit(ConsoleReportWorkAction::class.java) {
            discoveredErrors.set(task.discoveredErrors)
            outputToConsole.set(task.outputToConsole)
            ignoreFailures.set(task.ignoreFailures)
            verbose.set(task.verbose)
            generatedReportsPaths.from(loadedReporters.values)
            ktLintVersion.set(task.ktLintVersion)
            baseline.set(task.baseline)
            projectDirectory.set(projectLayout.projectDirectory)
        }
    }

    private fun loadLoadedReporters() = ObjectInputStream(
        FileInputStream(loadedReporters.asFile.get())
    ).use {
        @Suppress("UNCHECKED_CAST")
        it.readObject() as List<LoadedReporter>
    }

    private fun generateReporterOptions(
        loadedReporter: LoadedReporter
    ): Map<String, String> {
        val options = mutableMapOf(
            "verbose" to verbose.get().toString(),
            "color" to coloredOutput.get().toString()
        )
        if (outputColorName.get().isNotBlank()) {
            options["color_name"] = outputColorName.get()
        } else {
            // Same default as in the KtLint CLI
            options["color_name"] = "DARK_GRAY"
        }
        options.putAll(loadedReporter.reporterOptions)
        return options.toMap()
    }

    internal enum class LintType(
        val suffix: String
    ) {
        CHECK("Check"),
        FORMAT("Format")
    }

    internal companion object {
        internal fun generateNameForSourceSets(
            sourceSetName: String,
            lintType: LintType
        ): String = "ktlint${sourceSetName.capitalize()}SourceSet${lintType.suffix}"

        internal fun generateNameForKotlinScripts(
            lintType: LintType
        ): String = "ktlintKotlinScript${lintType.suffix}"

        const val DESCRIPTION = "Generates reports and prints errors into Gradle console."
    }
}
