package org.jlleitschuh.gradle.ktlint.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.konan.file.File
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jlleitschuh.gradle.ktlint.worker.ConsoleReportWorkAction
import org.jlleitschuh.gradle.ktlint.worker.GenerateReportsWorkAction
import org.jlleitschuh.gradle.ktlint.worker.LoadReportersWorkAction
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.util.concurrent.Callable
import javax.inject.Inject

/**
 * Generates reports and prints errors into Gradle console.
 *
 * This will actually fail the build in case some non-corrected lint issues.
 */
abstract class GenerateReportsTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    projectLayout: ProjectLayout,
    objectFactory: ObjectFactory
) : DefaultTask() {
    @get:Classpath
    internal abstract val ktLintClasspath: ConfigurableFileCollection

    @get:Classpath
    internal abstract val reportersClasspath: ConfigurableFileCollection

    @get:InputFile
    internal abstract val loadedReporterProviders: RegularFileProperty

    @get:InputFile
    internal abstract val loadedReporters: RegularFileProperty

    @get:Internal
    internal abstract val discoveredErrors: RegularFileProperty

    /**
     * Workaround for https://github.com/gradle/gradle/issues/2919
     */
    @Suppress("UnstableApiUsage", "Unused")
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal val discoveredErrorsWorkaround: FileCollection = projectLayout.files(
        Callable {
            val discoveredErrorsFile = discoveredErrors.asFile.orNull
            if (discoveredErrorsFile?.exists() == true) {
                discoveredErrorsFile
            } else {
                null
            }
        }
    )

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

    @Suppress("UnstableApiUsage")
    @get:OutputDirectory
    val reportsOutputDirectory: DirectoryProperty = objectFactory
        .directoryProperty()
        .convention(
            projectLayout.buildDirectory.dir("reports${File.separator}ktlint")
        )

    @Suppress("UnstableApiUsage")
    @TaskAction
    fun generateReports() {
        val queue = workerExecutor.classLoaderIsolation { spec ->
            spec.classpath.from(ktLintClasspath, reportersClasspath)
        }

        val loadedReporters = loadLoadedReporters()
        loadedReporters.forEach { loadedReporter ->
            queue.submit(GenerateReportsWorkAction::class.java) { param ->
                param.discoveredErrorsFile.set(discoveredErrors)
                param.loadedReporterProviders.set(loadedReporterProviders)
                param.reporterId.set(loadedReporter.reporterId)
                param.reporterOutput.set(
                    reportsOutputDirectory.file("${reportsName.get()}.${loadedReporter.fileExtension}")
                )
                param.reporterOptions.set(generateReporterOptions(loadedReporter))
            }
        }

        queue.submit(ConsoleReportWorkAction::class.java) { param ->
            param.discoveredErrors.set(discoveredErrors)
            param.outputToConsole.set(outputToConsole)
            param.ignoreFailures.set(ignoreFailures)
            param.verbose.set(verbose)
        }
    }

    private fun loadLoadedReporters() = ObjectInputStream(
        FileInputStream(loadedReporters.asFile.get())
    ).use {
        @Suppress("UNCHECKED_CAST")
        it.readObject() as List<LoadReportersWorkAction.LoadedReporter>
    }

    private fun generateReporterOptions(
        loadedReporter: LoadReportersWorkAction.LoadedReporter
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
        CHECK("Check"), FORMAT("Format")
    }

    companion object {
        internal fun generateNameForSourceSets(
            sourceSetName: String,
            lintType: LintType
        ): String {
            return "ktLint${sourceSetName.capitalize()}SourceSet${lintType.suffix}"
        }

        internal fun generateNameForKotlinScripts(
            lintType: LintType
        ): String {
            return "ktLintKotlinScript${lintType.suffix}"
        }

        const val DESCRIPTION = "Generates reports and prints errors into Gradle console."
    }
}
