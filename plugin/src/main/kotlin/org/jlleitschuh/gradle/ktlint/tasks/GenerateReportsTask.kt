package org.jlleitschuh.gradle.ktlint.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.FORMAT_KOTLIN_SCRIPT_TASK_NAME
import org.jlleitschuh.gradle.ktlint.KTLINT_CONFIGURATION_NAME
import org.jlleitschuh.gradle.ktlint.KTLINT_REPORTER_CONFIGURATION_NAME
import org.jlleitschuh.gradle.ktlint.KtlintExtension
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
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {
    @get:Classpath
    internal abstract val ktLintClasspath: ConfigurableFileCollection

    @get:Classpath
    internal abstract val reportersClasspath: ConfigurableFileCollection

    /**
     * We just need the path for relative path calculations, not an actual input
     */
    @get:Internal
    internal abstract val projectDirectory: DirectoryProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    internal abstract val loadedReporterProviders: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    internal abstract val loadedReporters: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    @get:Optional
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

    // Uses ConfigurableFileCollection instead of RegularFileProperty so that the file need not exist
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    @get:Optional
    internal abstract val baseline: ConfigurableFileCollection

    /**
     * Reports output directory.
     *
     * Default is "build/reports/ktlint/${taskName}/".
     */
    @get:OutputDirectory
    abstract val reportsOutputDirectory: DirectoryProperty

    @TaskAction
    fun generateReports() {
        // Classloader isolation is enough here as we just want to use some classes from KtLint classpath
        // to get errors and generate files/console reports. No KtLint main object is initialized/used in this case.
        val queue = workerExecutor.classLoaderIsolation {
            it.classpath.from(ktLintClasspath, reportersClasspath)
        }

        val loadedReporters = loadLoadedReporters()
            .associateWith {
                reportsOutputDirectory.file("${reportsName.get()}.${it.fileExtension}")
            }

        val task = this
        loadedReporters.forEach { (loadedReporter, reporterOutput) ->
            queue.submit(GenerateReportsWorkAction::class.java) {
                it.discoveredErrorsFile.set(task.discoveredErrors)
                it.loadedReporterProviders.set(task.loadedReporterProviders)
                it.reporterId.set(loadedReporter.reporterId)
                it.reporterOutput.set(reporterOutput)
                it.reporterOptions.set(generateReporterOptions(loadedReporter))
                it.ktLintVersion.set(task.ktLintVersion)
                it.baseline.set(task.baseline.files.firstOrNull())
                it.projectDirectory.set(task.projectDirectory)
                if (task.relative.get()) {
                    it.filePathsRelativeTo.set(projectDirectory.asFile)
                }
            }
        }

        queue.submit(ConsoleReportWorkAction::class.java) {
            it.discoveredErrors.set(task.discoveredErrors)
            it.outputToConsole.set(task.outputToConsole)
            it.ignoreFailures.set(task.ignoreFailures)
            it.verbose.set(verbose)
            it.generatedReportsPaths.from(loadedReporters.values)
            it.ktLintVersion.set(ktLintVersion)
            it.baseline.set(baseline.files.firstOrNull())
            it.projectDirectory.set(projectDirectory)
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

        fun registerCheckReportTaskForSourceSet(
            project: Project,
            lintTask: TaskProvider<out BaseKtLintCheckTask>,
            sourceSetName: String
        ): TaskProvider<GenerateReportsTask> {
            return register(project, lintTask, generateNameForSourceSets(sourceSetName, LintType.CHECK))
        }

        fun registerCheckReportTaskForScript(
            project: Project,
            lintTask: TaskProvider<out BaseKtLintCheckTask>
        ): TaskProvider<GenerateReportsTask> {
            return register(project, lintTask, generateNameForKotlinScripts(LintType.CHECK))
        }

        fun registerFormatReportTaskForSourceSet(
            project: Project,
            lintTask: TaskProvider<out BaseKtLintCheckTask>,
            sourceSetName: String
        ): TaskProvider<GenerateReportsTask> {
            return register(project, lintTask, generateNameForSourceSets(sourceSetName, LintType.FORMAT))
        }

        fun registerFormatReportTaskForScript(
            project: Project,
            lintTask: TaskProvider<out BaseKtLintCheckTask>
        ): TaskProvider<GenerateReportsTask> {
            return register(project, lintTask, generateNameForKotlinScripts(LintType.FORMAT))
        }

        private fun register(
            project: Project,
            lintTask: TaskProvider<out BaseKtLintCheckTask>,
            name: String
        ): TaskProvider<GenerateReportsTask> {
            val task = project.tasks.register(name, GenerateReportsTask::class.java) {
                it.projectDirectory.set(project.layout.projectDirectory)
                it.mustRunAfter(project.tasks.named(FORMAT_KOTLIN_SCRIPT_TASK_NAME))
                it.reportsName.set(name)
                it.commonConfiguration(project, lintTask)
                it.reportsOutputDirectory.set(
                    project.layout
                        .buildDirectory
                        .dir("reports${File.separator}ktlint${File.separator}$name")
                )
                val de = lintTask.flatMap { it.discoveredErrors }
                it.discoveredErrors.set(de)
                it.onlyIf { _ ->
                    de.map { it.asFile }.get().exists()
                }
            }
            return task
        }

        internal fun <T : BaseKtLintCheckTask> GenerateReportsTask.commonConfiguration(
            project: Project,
            lintTask: TaskProvider<T>
        ) {
            val ktlintExtension = project.extensions.getByType(KtlintExtension::class.java)
            val ktlintConfiguration = project.configurations.named(KTLINT_CONFIGURATION_NAME)
            val ktlintReporterConfiguration = project.configurations.named(KTLINT_REPORTER_CONFIGURATION_NAME)
            val loadReportersTask = project.tasks.named(LoadReportersTask.TASK_NAME, LoadReportersTask::class.java)
            description = DESCRIPTION
            dependsOn(lintTask)

            ktLintClasspath.setFrom(ktlintConfiguration)
            reportersClasspath.setFrom(ktlintReporterConfiguration)
            discoveredErrors.set(lintTask.get().discoveredErrors)
            loadedReporterProviders.set(loadReportersTask.flatMap { it.loadedReporterProviders })
            loadedReporters.set(loadReportersTask.flatMap { it.loadedReporters })
            enabledReporters.set(ktlintExtension.reporterExtension.reporters)
            outputToConsole.set(ktlintExtension.outputToConsole)
            coloredOutput.set(ktlintExtension.coloredOutput)
            outputColorName.set(ktlintExtension.outputColorName)
            ignoreFailures.set(ktlintExtension.ignoreFailures)
            verbose.set(ktlintExtension.verbose)
            ktLintVersion.set(ktlintExtension.version)
            relative.set(ktlintExtension.relative)
            baseline.from(ktlintExtension.baseline)
        }
    }
}
