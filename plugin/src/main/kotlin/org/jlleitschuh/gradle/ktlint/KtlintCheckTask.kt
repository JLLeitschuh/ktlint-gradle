package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
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
import javax.inject.Inject

@Suppress("UnstableApiUsage")
@CacheableTask
open class KtlintCheckTask @Inject constructor(
    private val objectFactory: ObjectFactory
) : SourceTask() {

    @get:Classpath
    internal val classpath: ConfigurableFileCollection = project.files()

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal val editorConfigFiles: FileCollection by lazy(LazyThreadSafetyMode.NONE) {
        // Gradle will lazy evaluate this task input only on task execution
        getEditorConfigFiles(project)
    }

    @get:Input
    internal val ktlintVersion: Property<String> = objectFactory.property()
    @get:Input
    internal val verbose: Property<Boolean> = objectFactory.property()
    @get:Input
    internal val ruleSets: ListProperty<String> = objectFactory.listProperty(String::class.java)
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

    @get:Internal
    internal val enabledReports
        get() = reporters.get()
            .map {
                KtlintReport(
                    objectFactory.property { set(it.isAvailable()) },
                    it,
                    newOutputFile().apply { set(it.getOutputFile()) }
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

        project.javaexec(generateJavaExecSpec(additionalConfig()))
    }

    protected open fun additionalConfig(): (JavaExecSpec) -> Unit = {}

    private fun generateJavaExecSpec(
        additionalConfig: (JavaExecSpec) -> Unit
    ): (JavaExecSpec) -> Unit = { javaExecSpec ->
        javaExecSpec.classpath = classpath
        javaExecSpec.main = resolveMainClassName()
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
        javaExecSpec.args(ruleSets.get().map { "--ruleset=$it" })
        javaExecSpec.isIgnoreExitValue = ignoreFailures.get()
        javaExecSpec.args(enabledReports.map { it.asArgument() })
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

    private fun resolveMainClassName() = when {
        SemVer.parse(ktlintVersion.get()) < SemVer(0, 32, 0) -> "com.github.shyiko.ktlint.Main"
        else -> "com.pinterest.ktlint.Main"
    }

    private fun ReporterType.isAvailable() =
        SemVer.parse(ktlintVersion.get()) >= availableSinceVersion

    private fun ReporterType.getOutputFile() =
        project.layout.buildDirectory.file(project.provider {
            "reports/ktlint/${this@KtlintCheckTask.name}.$fileExtension"
        })

    private fun FileTree.toRelativeFilesList(): List<File> {
        val baseDir = project.projectDir
        return files.map { it.relativeTo(baseDir) }
    }

    @get:OutputFiles
    val reportOutputFiles: Map<ReporterType, RegularFileProperty>
        get() = enabledReports.associateTo(mutableMapOf()) { it.reporterType to it.outputFile }
}
