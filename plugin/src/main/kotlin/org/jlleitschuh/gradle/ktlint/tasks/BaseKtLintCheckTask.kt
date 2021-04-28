package org.jlleitschuh.gradle.ktlint.tasks

import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileType
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
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.FILTER_INCLUDE_PROPERTY_NAME
import org.jlleitschuh.gradle.ktlint.KOTLIN_EXTENSIONS
import org.jlleitschuh.gradle.ktlint.applyGitFilter
import org.jlleitschuh.gradle.ktlint.getEditorConfigFiles
import org.jlleitschuh.gradle.ktlint.hookVersion
import org.jlleitschuh.gradle.ktlint.intermediateResultsBuildDir
import org.jlleitschuh.gradle.ktlint.property
import org.jlleitschuh.gradle.ktlint.worker.KtLintWorkAction
import java.io.File
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

    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal val editorConfigFiles: FileCollection = objectFactory.fileCollection().from(
        {
            getEditorConfigFiles(
                projectLayout.projectDirectory.asFile.toPath(),
                additionalEditorconfigFile
            )
        }
    )

    @get:Input
    internal abstract val ktLintVersion: Property<String>

    @get:Classpath
    internal abstract val ruleSetsClasspath: ConfigurableFileCollection

    @get:Input
    internal abstract val debug: Property<Boolean>

    @get:Input
    internal abstract val android: Property<Boolean>

    @get:Input
    internal abstract val disabledRules: SetProperty<String>

    @get:Input
    internal abstract val enableExperimentalRules: Property<Boolean>

    /**
     * Max lint worker heap size. Default is "256m".
     */
    @get:Internal
    val workerMaxHeapSize: Property<String> = objectFactory.property {
        convention("256m")
    }

    init {
        if (project.hasProperty(FILTER_INCLUDE_PROPERTY_NAME)) {
            // if FILTER_INCLUDE_PROPERTY_NAME exists then we are invoked from a git hook, check hook version
            if (project.findProperty(hookVersion) != hookVersion) {
                logger.warn("Your ktlint git hook is outdated, please update by running the addKtlint*GitPreCommitHook Gradle task.")
            }
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
        { source }
    )

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    internal abstract val loadedReporters: RegularFileProperty

    @get:OutputFile
    internal val discoveredErrors: RegularFileProperty = objectFactory
        .fileProperty()
        .convention(
            projectLayout.intermediateResultsBuildDir("${name}_errors.bin")
        )

    protected fun runLint(
        inputChanges: InputChanges?,
        formatSources: Boolean,
    ) {
        checkDisabledRulesSupportedKtLintVersion()

        val editorConfigUpdated = wasEditorConfigFilesUpdated(inputChanges)
        val filesToCheck = if (formatSources || editorConfigUpdated || inputChanges == null) {
            stableSources.files
        } else {
            getChangedSources(inputChanges)
        }

        logger.info("Executing ${if (inputChanges?.isIncremental == true) "incrementally" else "non-incrementally"}")
        logger.info("Editorconfig files were changed: $editorConfigUpdated")
        if (filesToCheck.isEmpty()) {
            logger.info("Skipping. No files to lint")
            didWork = false
            return
        } else {
            logger.debug("Linting files: ${filesToCheck.joinToString()}")
        }

        // Process isolation is used here to run KtLint in a separate java process.
        // This allows to better isolate work actions from different projects tasks between each other
        // and to not pollute Gradle daemon heap, which otherwise greatly increases GC time.
        val queue = workerExecutor.processIsolation { spec ->
            spec.classpath.from(ktLintClasspath, ruleSetsClasspath)
            spec.forkOptions { options ->
                options.maxHeapSize = workerMaxHeapSize.get()
            }
        }

        queue.submit(KtLintWorkAction::class.java) { params ->
            params.filesToLint.from(filesToCheck)
            params.enableExperimental.set(enableExperimentalRules)
            params.android.set(android)
            params.disabledRules.set(disabledRules)
            params.debug.set(debug)
            params.additionalEditorconfigFile.set(additionalEditorconfigFile)
            params.formatSource.set(formatSources)
            params.discoveredErrorsFile.set(discoveredErrors)
            params.ktLintVersion.set(ktLintVersion)
            params.editorconfigFilesWereChanged.set(editorConfigUpdated)
        }
    }

    private fun wasEditorConfigFilesUpdated(
        inputChanges: InputChanges?
    ) = inputChanges != null &&
        inputChanges.isIncremental &&
        !inputChanges.getFileChanges(editorConfigFiles).none()

    private fun getChangedSources(
        inputChanges: InputChanges
    ): Set<File> = inputChanges
        .getFileChanges(stableSources)
        .asSequence()
        .filter {
            it.fileType != FileType.DIRECTORY &&
                it.changeType != ChangeType.REMOVED
        }
        .map { it.file }
        .toSet()

    private fun checkDisabledRulesSupportedKtLintVersion() {
        if (disabledRules.get().isNotEmpty() &&
            SemVer.parse(ktLintVersion.get()) < SemVer(0, 34, 2)
        ) {
            throw GradleException("Rules disabling is supported since 0.34.2 ktlint version.")
        }
    }
}
