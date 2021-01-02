package org.jlleitschuh.gradle.ktlint.tasks

import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.model.ReplacedBy
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceTask
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.FILTER_INCLUDE_PROPERTY_NAME
import org.jlleitschuh.gradle.ktlint.KOTLIN_EXTENSIONS
import org.jlleitschuh.gradle.ktlint.applyGitFilter
import org.jlleitschuh.gradle.ktlint.getEditorConfigFiles
import org.jlleitschuh.gradle.ktlint.intermediateResultsBuildDir
import org.jlleitschuh.gradle.ktlint.worker.KtLintWorkAction
import java.io.File
import java.util.concurrent.Callable
import javax.inject.Inject

@Suppress("UnstableApiUsage")
abstract class BaseKtLintCheckTask @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    private val workerExecutor: WorkerExecutor,
) : SourceTask() {

    @get:Classpath
    internal abstract val ktLintClasspath: ConfigurableFileCollection

    @get:Internal
    internal abstract val additionalEditorconfigFile: RegularFileProperty

    @Suppress("unused")
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal val editorConfigFiles: FileCollection by lazy(LazyThreadSafetyMode.NONE) {
        // Gradle will lazy evaluate this task input only on task execution
        getEditorConfigFiles(project, additionalEditorconfigFile)
    }

    @get:Input
    internal abstract val ktlintVersion: Property<String>

    @get:Classpath
    internal abstract val ruleSetsClasspath: ConfigurableFileCollection

    @get:Input
    internal abstract val debug: Property<Boolean>

    @get:Input
    internal abstract val android: Property<Boolean>

    @get:Input
    internal abstract val disabledRules: SetProperty<String>

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
        Callable {
            return@Callable source
        }
    )

    @get:InputFile
    internal abstract val loadedRuleSets: RegularFileProperty

    @get:InputFile
    internal abstract val loadedReporters: RegularFileProperty

    @get:OutputFile
    internal val discoveredErrors: RegularFileProperty = objectFactory
        .fileProperty()
        .convention(
            projectLayout.intermediateResultsBuildDir("${name}_errors.bin")
        )

    protected fun runLint(
        filesToCheck: Set<File>,
        formatSources: Boolean,
    ) {
        checkMinimalSupportedKtLintVersion()
        checkCWEKtLintVersion()

        val queue = workerExecutor.classLoaderIsolation { workerExecutor ->
            workerExecutor.classpath.from(ktLintClasspath, ruleSetsClasspath)
        }

        queue.submit(KtLintWorkAction::class.java) { params ->
            params.filesToLint.from(filesToCheck)
            params.loadedRuleSets.set(loadedRuleSets)
            params.android.set(android)
            params.disabledRules.set(disabledRules)
            params.debug.set(debug)
            params.additionalEditorconfigFile.set(additionalEditorconfigFile)
            params.formatSource.set(formatSources)
            params.discoveredErrorsFile.set(discoveredErrors)
        }
    }

    private fun checkMinimalSupportedKtLintVersion() {
        if (SemVer.parse(ktlintVersion.get()) < SemVer(0, 22, 0)) {
            throw GradleException(
                "Ktlint versions less than 0.22.0 are not supported. " +
                    "Detected Ktlint version: ${ktlintVersion.get()}."
            )
        }
    }

    private fun checkCWEKtLintVersion() {
        if (!ruleSetsClasspath.isEmpty &&
            SemVer.parse(ktlintVersion.get()) < SemVer(0, 30, 0)
        ) {
            logger.warn(
                "You are using ktlint version ${ktlintVersion.get()} that has the security vulnerability " +
                    "'CWE-494: Download of Code Without Integrity Check'.${System.lineSeparator()}" +
                    "Consider upgrading to versions consider upgrading to versions >= 0.30.0"
            )
        }
    }
}
