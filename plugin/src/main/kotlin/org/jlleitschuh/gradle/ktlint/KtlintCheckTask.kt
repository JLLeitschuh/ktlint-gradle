package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.DefaultTask
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
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec
import org.jlleitschuh.gradle.ktlint.reporter.KtlintReport
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import javax.inject.Inject

@CacheableTask
open class KtlintCheckTask @Inject constructor(
    private val objectFactory: ObjectFactory
) : DefaultTask() {

    @get:Classpath
    val classpath: ConfigurableFileCollection = project.files()

    @get:Internal
    val sourceDirectories: ConfigurableFileCollection = project.files()

    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val sources: FileTree = sourceDirectories.asFileTree.matching { filterable ->
        KOTLIN_EXTENSIONS.forEach {
            filterable.include("**/*.$it")
        }
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal val editorConfigFiles: FileCollection = getEditorConfigFiles(project)

    @get:Input
    val ktlintVersion: Property<String> = objectFactory.property()
    @get:Input
    val verbose: Property<Boolean> = objectFactory.property()
    @get:Input
    val ruleSets: ListProperty<String> = objectFactory.listProperty(String::class.java)
    @get:Input
    val debug: Property<Boolean> = objectFactory.property()
    @get:Input
    val android: Property<Boolean> = objectFactory.property()
    @get:Input
    val ignoreFailures: Property<Boolean> = objectFactory.property()
    @get:Internal
    val reporters: SetProperty<ReporterType> = objectFactory.setProperty()
    @get:Console
    val outputToConsole: Property<Boolean> = objectFactory.property()

    @get:Internal
    val enabledReports
        get() = reporters.get()
            .map {
                KtlintReport(
                    objectFactory.property() { set(it.isAvailable()) },
                    it,
                    newOutputFile().apply { set(it.getOutputFile()) }
                )
            }
            .filter { it.enabled.get() }

    @TaskAction
    fun lint() {
        checkMinimalSupportedKtlintVersion()
        checkOutputPathsWithSpacesSupported()

        project.javaexec(generateJavaExecSpec(additionalConfig()))
    }

    protected open fun additionalConfig(): (JavaExecSpec) -> Unit = {}

    private fun generateJavaExecSpec(
        additionalConfig: (JavaExecSpec) -> Unit
    ): (JavaExecSpec) -> Unit = { javaExecSpec ->
        javaExecSpec.classpath = classpath
        javaExecSpec.main = "com.github.shyiko.ktlint.Main"
        javaExecSpec.args(sourceDirectories.flatMap { dir ->
            KOTLIN_EXTENSIONS.map { extension ->
                "${dir.path}/**/*.$extension"
            }
        })
        if (verbose.get()) {
            javaExecSpec.args("--verbose")
        }
        if (debug.get()) {
            javaExecSpec.args("--debug")
        }
        if (android.get() && ktlintVersion.isAndroidFlagAvailable()) {
            javaExecSpec.args("--android")
        }
        if (outputToConsole.get()) {
            javaExecSpec.args("--reporter=plain")
        }
        javaExecSpec.args(ruleSets.get().map { "--ruleset=$it" })
        javaExecSpec.isIgnoreExitValue = ignoreFailures.get()
        javaExecSpec.args(enabledReports.map { it.asArgument() })
        additionalConfig(javaExecSpec)
    }

    private fun checkMinimalSupportedKtlintVersion() {
        if (SemVer.parse(ktlintVersion.get()) < SemVer(0, 10, 0)) {
            throw GradleException("Ktlint versions less than 0.10.0 are not supported." +
                "Detected Ktlint version: ${ktlintVersion.get()}.")
        }
    }

    private fun checkOutputPathsWithSpacesSupported() {
        if (SemVer.parse(ktlintVersion.get()) < SemVer(0, 20, 0)) {
            enabledReports.forEach {
                val reportOutputPath = it.outputFile.get().asFile.absolutePath
                if (reportOutputPath.contains(" ")) {
                    throw GradleException(
                        "The output path passed `$reportOutputPath` contains spaces. Ktlint versions less than 0.20.0 do not support this.")
                }
            }
        }
    }

    private fun ReporterType.isAvailable() =
        SemVer.parse(ktlintVersion.get()) >= availableSinceVersion

    private fun ReporterType.getOutputFile() =
        project.layout.buildDirectory.file(project.provider {
            "reports/ktlint/${this@KtlintCheckTask.name}.$fileExtension"
        })

    @get:OutputFiles
    val reportOutputFiles: Map<ReporterType, RegularFileProperty>
        get() = enabledReports.associateTo(mutableMapOf()) { it.reporterType to it.outputFile }
}