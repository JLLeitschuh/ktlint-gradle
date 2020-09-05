package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.jlleitschuh.gradle.ktlint.reporter.CustomReporter
import org.jlleitschuh.gradle.ktlint.reporter.KtlintReport
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.Callable

@Suppress("UnstableApiUsage")
abstract class BaseKtlintCheckTask(
    private val objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout
) : SourceTask() {

    @get:Classpath
    internal val classpath: ConfigurableFileCollection = project.files()

    @get:Internal
    internal val additionalEditorconfigFile: RegularFileProperty = objectFactory.fileProperty()

    @Suppress("unused")
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal val editorConfigFiles: FileCollection by lazy(LazyThreadSafetyMode.NONE) {
        // Gradle will lazy evaluate this task input only on task execution
        getEditorConfigFiles(project, additionalEditorconfigFile)
    }

    @get:Input
    internal val ktlintVersion: Property<String> = objectFactory.property()
    @get:Input
    internal val verbose: Property<Boolean> = objectFactory.property()
    @get:Classpath
    internal val ruleSetsClasspath: ConfigurableFileCollection = project.files()
    @get:Input
    internal val debug: Property<Boolean> = objectFactory.property()
    @get:Input
    internal val android: Property<Boolean> = objectFactory.property()
    @get:Input
    internal val ignoreFailures: Property<Boolean> = objectFactory.property()
    @get:Internal
    internal val reporters: SetProperty<ReporterType> = objectFactory.setProperty()
    @get:Classpath
    internal val customReportersClasspath: ConfigurableFileCollection = project.files()
    @get:Internal
    internal val customReporters: SetProperty<CustomReporter> = objectFactory.setProperty()
    @get:Console
    internal val outputToConsole: Property<Boolean> = objectFactory.property()
    @get:Console
    internal val coloredOutput: Property<Boolean> = objectFactory.property()
    @get:Input
    internal val outputColorName: Property<String> = objectFactory.property()
    @get:Input
    internal val enableExperimentalRules: Property<Boolean> = objectFactory.property()
    @get:Input
    internal val disabledRules: SetProperty<String> = objectFactory.setProperty()

    /**
     * Should only be invoked at configuration time.
     */
    private fun getEnabledReports(): List<KtlintReport.BuiltIn> =
        reporters
            .get()
            .ifEmpty { setOf(ReporterType.PLAIN) }
            .map {
                KtlintReport.BuiltIn(
                    it.reporterName,
                    objectFactory.property { set(it.isAvailable()) },
                    it,
                    objectFactory.fileProperty().apply {
                        set(it.getOutputFile())
                    }
                )
            }
            .filter { it.enabled.get() }

    /**
     * Should only be invoked at configuration time.
     */
    private fun getCustomReports(): List<KtlintReport.CustomReport> =
        customReporters
            .get()
            .map {
                KtlintReport.CustomReport(
                    it.reporterId,
                    objectFactory.fileProperty().apply {
                        set(
                            project.configurations.getByName(KTLINT_REPORTER_CONFIGURATION_NAME)
                                .resolvedConfiguration
                                .resolvedArtifacts
                                .find { artifact ->
                                    artifact.name == it.dependencyArtifact.name
                                }
                                ?.file ?: throw GradleException("Failed to resolve ${it.dependencyArtifact} artifact")
                        )
                    },
                    objectFactory.fileProperty().apply {
                        set(it.getOutputFile())
                    }
                )
            }

    @get:Nested
    internal val allReports by lazy {
        // This is called at configuration time and stored for use at execution time.
        getEnabledReports().plus(getCustomReports())
    }

    init {
        if (project.hasProperty(FILTER_INCLUDE_PROPERTY_NAME)) {
            applyGitFilter()
        } else {
            KOTLIN_EXTENSIONS.forEach {
                include("**/*.$it")
            }
        }
    }

    @ReplacedBy("stableSources")
    override fun getSource(): FileTree {
        return super.getSource()
    }

    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal val stableSources: FileCollection = project.files(
        Callable<FileTree> {
            return@Callable getSource()
        }
    )

    @get:Internal
    lateinit var runner: KtLintRunner

    protected fun runLint(
        filesToCheck: Set<File>
    ) {
        checkMinimalSupportedKtlintVersion()
        checkCWEKtlintVersion()
        checkExperimentalRulesSupportedKtlintVersion()
        checkDisabledRulesSupportedKtlintVersion()

        val argsFile = writeArgsFile(filesToCheck, additionalConfig())
        runner.lint(classpath, ktlintVersion.get(), ignoreFailures.get(), argsFile)
    }

    private fun writeArgsFile(filesToCheck: Set<File>, additionalConfig: (PrintWriter) -> Unit): File {
        val argsConfigFile = ktlintArgsFile.get().asFile

        if (!argsConfigFile.exists()) {
            argsConfigFile.parentFile.mkdirs()
            argsConfigFile.createNewFile()
        }
        argsConfigFile.printWriter().use { argsWriter ->
            if (verbose.get()) argsWriter.println("--verbose")
            if (debug.get()) argsWriter.println("--debug")
            if (android.get()) argsWriter.println("--android")
            if (outputToConsole.get()) argsWriter.println("--reporter=plain")
            if (coloredOutput.get()) argsWriter.println("--color")
            if (enableExperimentalRules.get()) argsWriter.println("--experimental")
            if (additionalEditorconfigFile.isPresent) {
                argsWriter.println("--editorconfig=${additionalEditorconfigFile.get().asFile.absolutePath}")
            }
            ruleSetsClasspath
                .files
                .map { "--ruleset=${it.absolutePath}" }
                .forEach { argsWriter.println(it) }
            allReports
                .map { it.asArgument() }
                .forEach { argsWriter.println(it) }
            disabledRules
                .get()
                .joinToString(separator = ",")
                .run {
                    if (isNotEmpty()) argsWriter.println("--disabled_rules=$this")
                }
            outputColorName
                .get()
                .run {
                    if (isNotEmpty()) argsWriter.println("--color-name=$this")
                }
            additionalConfig(argsWriter)
            filesToCheck.forEach {
                argsWriter.println("\"${it.toRelativeFile()}\"")
            }
        }
        return argsConfigFile
    }

    @OutputFiles
    val ktlintArgsFile = objectFactory.fileProperty().apply {
        set(
            projectLayout.buildDirectory.file(
                project.provider {
                    "ktlint/${this@BaseKtlintCheckTask.name}.args"
                }
            )
        )
    }

    abstract fun additionalConfig(): (PrintWriter) -> Unit

    private fun checkMinimalSupportedKtlintVersion() {
        if (SemVer.parse(ktlintVersion.get()) < SemVer(0, 22, 0)) {
            throw GradleException(
                "Ktlint versions less than 0.22.0 are not supported. " +
                    "Detected Ktlint version: ${ktlintVersion.get()}."
            )
        }
    }

    private fun checkCWEKtlintVersion() {
        if (!ruleSetsClasspath.isEmpty &&
            SemVer.parse(ktlintVersion.get()) < SemVer(0, 30, 0)
        ) {
            logger.warn(
                "You are using ktlint version ${ktlintVersion.get()} that has the security vulnerability " +
                    "'CWE-494: Download of Code Without Integrity Check'.\n" +
                    "Consider upgrading to versions consider upgrading to versions >= 0.30.0"
            )
        }
    }

    private fun checkExperimentalRulesSupportedKtlintVersion() {
        if (enableExperimentalRules.get() &&
            SemVer.parse(ktlintVersion.get()) < SemVer(0, 31, 0)
        ) {
            throw GradleException("Experimental rules are supported since 0.31.0 ktlint version.")
        }
    }

    private fun checkDisabledRulesSupportedKtlintVersion() {
        if (disabledRules.get().isNotEmpty() &&
            SemVer.parse(ktlintVersion.get()) < SemVer(0, 34, 2)
        ) {
            throw GradleException("Rules disabling is supported since 0.34.2 ktlint version.")
        }
    }

    private fun ReporterType.isAvailable() =
        SemVer.parse(ktlintVersion.get()) >= availableSinceVersion

    private fun ReporterType.getOutputFile() = reporterOutputDir.map {
        it.file("${this@BaseKtlintCheckTask.name}.$fileExtension")
    }

    private fun CustomReporter.getOutputFile() = reporterOutputDir.map {
        it.file("${this@BaseKtlintCheckTask.name}.$fileExtension")
    }

    private fun File.toRelativeFile(): File = relativeTo(projectLayout.projectDirectory.asFile)

    /**
     * Base location of ktlint generated reports.
     *
     * Default is "${project.buildDir}/reports/ktlint/$name".
     *
     * **Note**: should be unique per task, otherwise task caching will not work.
     */
    @get:OutputDirectory
    val reporterOutputDir: DirectoryProperty = objectFactory.directoryProperty().convention(
        project.layout.buildDirectory.dir("reports/ktlint/$name")
    )

    /**
     * Provides all reports outputs map: reporter id to reporter output file.
     */
    @Suppress("unused")
    @get:OutputFiles
    val allReportsOutputFiles: Map<String, RegularFileProperty>
        get() = allReports.associateTo(mutableMapOf()) { it.reporterId to it.outputFile }
}
