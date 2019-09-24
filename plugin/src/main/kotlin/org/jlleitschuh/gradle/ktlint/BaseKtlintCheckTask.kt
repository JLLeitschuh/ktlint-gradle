package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
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
import org.jlleitschuh.gradle.ktlint.reporter.KtlintReport
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import java.io.File
import java.io.PrintWriter

@Suppress("UnstableApiUsage")
abstract class BaseKtlintCheckTask(
    private val objectFactory: ObjectFactory
) : SourceTask() {

    @get:Classpath
    internal val classpath: ConfigurableFileCollection = project.files()

    @get:Internal
    internal val additionalEditorconfigFile: RegularFileProperty = objectFactory.fileProperty()

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
    @get:Console
    internal val outputToConsole: Property<Boolean> = objectFactory.property()
    @get:Console
    internal val coloredOutput: Property<Boolean> = objectFactory.property()
    @get:Input
    internal val enableExperimentalRules: Property<Boolean> = objectFactory.property()
    @get:Input
    internal val disabledRules: SetProperty<String> = objectFactory.setProperty()

    @get:Internal
    internal val enabledReports
        get() = reporters.get()
            .map {
                KtlintReport(
                    objectFactory.property { set(it.isAvailable()) },
                    it,
                    objectFactory.fileProperty().apply {
                        set(it.getOutputFile())
                    }
                )
            }
            .filter { it.enabled.get() }

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
        checkDisabledRulesSupportedKtlintVersion()

        project.javaexec(generateJavaExecSpec(additionalConfig()))
    }

    @OutputFiles
    private val ktlintArgsFile = objectFactory.fileProperty().apply {
        set(
            project.layout.buildDirectory.file(
                project.provider {
                    "ktlint/${this@BaseKtlintCheckTask.name}.args"
                }
            )
        )
    }

    abstract fun additionalConfig(): (PrintWriter) -> Unit

    private fun generateJavaExecSpec(
        additionalConfig: (PrintWriter) -> Unit
    ): (JavaExecSpec) -> Unit = { javaExecSpec ->
        javaExecSpec.classpath = classpath
        javaExecSpec.main = resolveMainClassName(ktlintVersion.get())
        javaExecSpec.isIgnoreExitValue = ignoreFailures.get()

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
            enabledReports
                .map { it.asArgument() }
                .forEach { argsWriter.println(it) }
            disabledRules
                .get()
                .joinToString(separator = ",")
                .run {
                    if (isNotEmpty()) argsWriter.println("--disabled_rules=$this")
                }
            getSource()
                .toRelativeFilesList()
                .forEach { argsWriter.println(it) }
            additionalConfig(argsWriter)
        }
        javaExecSpec.args("@$argsConfigFile")
    }

    private fun checkMinimalSupportedKtlintVersion() {
        if (SemVer.parse(ktlintVersion.get()) < SemVer(0, 22, 0)) {
            throw GradleException("Ktlint versions less than 0.22.0 are not supported. " +
                "Detected Ktlint version: ${ktlintVersion.get()}.")
        }
    }

    private fun checkCWEKtlintVersion() {
        if (!ruleSetsClasspath.isEmpty &&
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

    private fun checkDisabledRulesSupportedKtlintVersion() {
        if (disabledRules.get().isNotEmpty() &&
            SemVer.parse(ktlintVersion.get()) < SemVer(0, 34, 2)) {
            throw GradleException("Rules disabling is supported since 0.34.2 ktlint version.")
        }
    }

    private fun ReporterType.isAvailable() =
        SemVer.parse(ktlintVersion.get()) >= availableSinceVersion

    private fun ReporterType.getOutputFile() =
        project.layout.buildDirectory.file(project.provider {
            "reports/ktlint/${this@BaseKtlintCheckTask.name}.$fileExtension"
        })

    private fun FileTree.toRelativeFilesList(): List<File> {
        val baseDir = project.projectDir
        return files.map { it.relativeTo(baseDir) }
    }

    @get:OutputFiles
    val reportOutputFiles: Map<ReporterType, RegularFileProperty>
        get() = enabledReports.associateTo(mutableMapOf()) { it.reporterType to it.outputFile }
}
