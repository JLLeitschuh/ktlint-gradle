package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec
import org.jlleitschuh.gradle.ktlint.reporter.CustomReporter
import org.jlleitschuh.gradle.ktlint.reporter.KtlintReport
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.io.File

@Suppress("UnstableApiUsage")
abstract class BaseKtlintCheckTask(
    private val objectFactory: ObjectFactory,
    private val projectLayout: ProjectLayout
) : SourceTask() {

    @get:Classpath
    internal val classpath: ConfigurableFileCollection = project.files()

    @get:Internal
    internal val additionalEditorconfigFile: RegularFileProperty = newFileProperty(objectFactory, projectLayout)

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
    @get:Input
    internal val ruleSets: ListProperty<String> = objectFactory.listProperty(String::class.java)
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
    internal val enableExperimentalRules: Property<Boolean> = objectFactory.property()

    @get:Internal
    internal val enabledReports: List<KtlintReport.BuiltIn>
        get() = reporters.get()
            .map {
                KtlintReport.BuiltIn(
                    it.reporterName,
                    objectFactory.property { set(it.isAvailable()) },
                    it,
                    newFileProperty(objectFactory, projectLayout).apply {
                        set(it.getOutputFile())
                    }
                )
            }
            .filter { it.enabled.get() }

    @get:Internal
    internal val customReports: List<KtlintReport.CustomReport>
        get() = customReporters.get()
            .map {
                KtlintReport.CustomReport(
                    it.reporterId,
                    newFileProperty(objectFactory, projectLayout).apply {
                        set(
                            project.configurations.getByName(KTLINT_REPORTER_CONFIGURATION_NAME)
                                .resolvedConfiguration
                                .resolvedArtifacts
                                .find { artifact ->
                                    artifact.name == it.dependency.name
                                }
                                ?.file ?: throw GradleException("Failed to resolve ${it.dependency} artifact")
                        )
                    },
                    newFileProperty(objectFactory, projectLayout).apply {
                        set(it.getOutputFile())
                    }
                )
            }

    @get:Internal
    internal val allReports
        get() = enabledReports.plus(customReports)

    init {
        if (project.hasProperty(FILTER_INCLUDE_PROPERTY_NAME)) {
            applyGitFilter()
        } else {
            KOTLIN_EXTENSIONS.forEach {
                include("**/*.$it")
            }
        }
    }

    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource(): FileTree { return super.getSource() }

    @TaskAction
    fun lint() {
        checkMinimalSupportedKtlintVersion()
        checkCWEKtlintVersion()
        checkExperimentalRulesSupportedKtlintVersion()

        project.javaexec(generateJavaExecSpec(additionalConfig()))
    }

    abstract fun additionalConfig(): (JavaExecSpec) -> Unit

    private fun generateJavaExecSpec(
        additionalConfig: (JavaExecSpec) -> Unit
    ): (JavaExecSpec) -> Unit = { javaExecSpec ->
        javaExecSpec.classpath = classpath
        javaExecSpec.main = resolveMainClassName(ktlintVersion.get())
        javaExecSpec.args(getSource().toRelativeFilesList())
        if (verbose.get()) {
            javaExecSpec.args("--verbose")
        }
        if (debug.get()) {
            javaExecSpec.args("--debug")
        }
        if (android.get()) {
            javaExecSpec.args("--android")
        }
        if (outputToConsole.get()) {
            javaExecSpec.args("--reporter=plain")
        }
        if (coloredOutput.get()) {
            javaExecSpec.args("--color")
        }
        if (enableExperimentalRules.get()) {
            javaExecSpec.args("--experimental")
        }
        if (additionalEditorconfigFile.isPresent) {
            javaExecSpec.args("--editorconfig=${additionalEditorconfigFile.get().asFile.absolutePath}")
        }
        javaExecSpec.args(ruleSets.get().map { "--ruleset=$it" })
        javaExecSpec.args(ruleSetsClasspath.files.map { "--ruleset=${it.absolutePath}" })
        javaExecSpec.isIgnoreExitValue = ignoreFailures.get()
        javaExecSpec.args(allReports.map { it.asArgument() })
        additionalConfig(javaExecSpec)
    }

    private fun checkMinimalSupportedKtlintVersion() {
        if (SemVer.parse(ktlintVersion.get()) < SemVer(0, 22, 0)) {
            throw GradleException("Ktlint versions less than 0.22.0 are not supported. " +
                "Detected Ktlint version: ${ktlintVersion.get()}.")
        }
    }

    private fun checkCWEKtlintVersion() {
        if (!ruleSets.get().isNullOrEmpty() &&
            SemVer.parse(ktlintVersion.get()) < SemVer(0, 30, 0)) {
            logger.warn(
                "You are using ktlint version ${ktlintVersion.get()} that has the security vulnerability " +
                    "'CWE-494: Download of Code Without Integrity Check'.\n" +
                    "Consider upgrading to versions consider upgrading to versions >= 0.30.0"
            )
        }
    }

    private fun checkExperimentalRulesSupportedKtlintVersion() {
        if (enableExperimentalRules.get() &&
            SemVer.parse(ktlintVersion.get()) < SemVer(0, 31, 0)) {
            throw GradleException("Experimental rules are supported since 0.31.0 ktlint version.")
        }
    }

    private fun ReporterType.isAvailable() =
        SemVer.parse(ktlintVersion.get()) >= availableSinceVersion

    private fun ReporterType.getOutputFile() =
        project.layout.buildDirectory.file(project.provider {
            "reports/ktlint/${this@BaseKtlintCheckTask.name}.$fileExtension"
        })

    private fun CustomReporter.getOutputFile() =
        project.layout.buildDirectory.file(project.provider {
            "reports/ktlint/${this@BaseKtlintCheckTask.name}.$reporterFileExtension"
        })

    private fun FileTree.toRelativeFilesList(): List<File> {
        val baseDir = project.projectDir
        return files.map { it.relativeTo(baseDir) }
    }

    @Deprecated(
        "Please use allREportOutputFiles",
        ReplaceWith("allReportOutputFiles")
    )
    @get:OutputFiles
    val reportOutputFiles: Map<ReporterType, RegularFileProperty>
        get() = enabledReports.associateTo(mutableMapOf()) { it.reporterType to it.outputFile }

    /**
     * Provides all reports outputs map: reporter id to reporter output file.
     */
    @get:OutputFiles
    val allReportOutputFiles: Map<String, RegularFileProperty>
        get() = allReports.associateTo(mutableMapOf()) { it.reporterId to it.outputFile }
}
