package org.jlleitschuh.gradle.ktlint.tasks

import groovy.lang.Closure
import net.swiftzer.semver.SemVer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTreeElement
import org.gradle.api.file.FileType
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.FILTER_INCLUDE_PROPERTY_NAME
import org.jlleitschuh.gradle.ktlint.KOTLIN_EXTENSIONS
import org.jlleitschuh.gradle.ktlint.applyGitFilter
import org.jlleitschuh.gradle.ktlint.getEditorConfigFiles
import org.jlleitschuh.gradle.ktlint.intermediateResultsBuildDir
import org.jlleitschuh.gradle.ktlint.property
import org.jlleitschuh.gradle.ktlint.worker.KtLintWorkAction
import java.io.File
import javax.inject.Inject

@Suppress("UnstableApiUsage")
abstract class BaseKtLintCheckTask @Inject constructor(
    private val objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    private val workerExecutor: WorkerExecutor,
    private val patternFilterable: PatternFilterable
) : DefaultTask(),
    PatternFilterable {

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

    private var sourceFiles: ConfigurableFileCollection = objectFactory.fileCollection()

    init {
        if (project.hasProperty(FILTER_INCLUDE_PROPERTY_NAME)) {
            applyGitFilter()
        } else {
            KOTLIN_EXTENSIONS.forEach {
                include("**/*.$it")
            }
        }
    }

    @get:IgnoreEmptyDirectories
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val source: FileCollection = objectFactory
        .fileCollection()
        .from({ sourceFiles.asFileTree.matching(patternFilterable) })

    /**
     * Sets the source from this task.
     *
     * @param source given source objects will be evaluated as per [org.gradle.api.Project.file].
     */
    fun setSource(source: Any): BaseKtLintCheckTask {
        sourceFiles = objectFactory.fileCollection().from(source)
        return this
    }

    /**
     * Adds some source to this task.
     *
     * @param sources given source objects will be evaluated as per [org.gradle.api.Project.files].
     */
    fun source(vararg sources: Any): BaseKtLintCheckTask {
        sourceFiles.from(sources)
        return this
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    internal abstract val loadedReporters: RegularFileProperty

    @get:OutputFile
    internal val discoveredErrors: RegularFileProperty = objectFactory
        .fileProperty()
        .convention(
            projectLayout.intermediateResultsBuildDir("${name}_errors.bin")
        )

    @Internal
    override fun getIncludes(): MutableSet<String> = patternFilterable.includes
    @Internal
    override fun getExcludes(): MutableSet<String> = patternFilterable.excludes

    override fun setIncludes(includes: MutableIterable<String>): BaseKtLintCheckTask =
        also { patternFilterable.setIncludes(includes) }

    override fun setExcludes(excludes: MutableIterable<String>): BaseKtLintCheckTask =
        also { patternFilterable.setExcludes(excludes) }

    override fun include(vararg includes: String?): BaseKtLintCheckTask =
        also { patternFilterable.include(*includes) }

    override fun include(includes: MutableIterable<String>): BaseKtLintCheckTask =
        also { patternFilterable.include(includes) }

    override fun include(includeSpec: Spec<FileTreeElement>): BaseKtLintCheckTask =
        also { patternFilterable.include(includeSpec) }

    override fun include(includeSpec: Closure<*>): BaseKtLintCheckTask =
        also { patternFilterable.include(includeSpec) }

    override fun exclude(vararg excludes: String?): BaseKtLintCheckTask =
        also { patternFilterable.exclude(*excludes) }

    override fun exclude(excludes: MutableIterable<String>): BaseKtLintCheckTask =
        also { patternFilterable.exclude(excludes) }

    override fun exclude(excludeSpec: Spec<FileTreeElement>): BaseKtLintCheckTask =
        also { patternFilterable.exclude(excludeSpec) }

    override fun exclude(excludeSpec: Closure<*>): BaseKtLintCheckTask =
        also { patternFilterable.exclude(excludeSpec) }

    protected fun runLint(
        inputChanges: InputChanges
    ) {
        checkDisabledRulesSupportedKtLintVersion()

        val editorConfigUpdated = wasEditorConfigFilesUpdated(inputChanges)
        val filesToCheck = if (editorConfigUpdated) {
            source.files
        } else {
            getChangedSources(inputChanges)
        }

        logTaskExecutionState(inputChanges, editorConfigUpdated)
        if (skipExecution(filesToCheck)) return

        submitKtLintWork(filesToCheck, false, editorConfigUpdated, null)
    }

    protected fun runFormat(
        inputChanges: InputChanges,
        formatSnapshot: File
    ) {
        checkDisabledRulesSupportedKtLintVersion()

        val editorConfigUpdated = wasEditorConfigFilesUpdated(inputChanges)
        val filesToCheck = if (editorConfigUpdated) {
            source.files
        } else {
            val snapshot = if (formatSnapshot.exists()) {
                KtLintWorkAction.FormatTaskSnapshot.readFromFile(formatSnapshot)
            } else {
                KtLintWorkAction.FormatTaskSnapshot(emptyMap())
            }
            val formattedSources = snapshot.formattedSources.keys.filter { it.exists() }

            getChangedSources(inputChanges) + formattedSources
        }

        logTaskExecutionState(inputChanges, editorConfigUpdated)
        if (skipExecution(filesToCheck)) return

        submitKtLintWork(filesToCheck, true, editorConfigUpdated, formatSnapshot)
    }

    private fun logTaskExecutionState(
        inputChanges: InputChanges,
        editorConfigUpdated: Boolean,
    ) {
        logger.info("Executing ${if (inputChanges.isIncremental) "incrementally" else "non-incrementally"}")
        logger.info("Editorconfig files were changed: $editorConfigUpdated")
    }

    private fun skipExecution(filesToCheck: Set<File>): Boolean {
        return if (filesToCheck.isEmpty()) {
            logger.info("Skipping. No files to lint")
            didWork = false
            true
        } else {
            logger.debug("Linting files: ${filesToCheck.joinToString()}")
            false
        }
    }

    private fun submitKtLintWork(
        filesToCheck: Set<File>,
        formatSources: Boolean,
        editorConfigUpdated: Boolean,
        formatSnapshot: File? = null
    ) {
        // Process isolation is used here to run KtLint in a separate java process.
        // This allows to better isolate work actions from different projects tasks between each other
        // and to not pollute Gradle daemon heap, which otherwise greatly increases GC time.
        val queue = workerExecutor.processIsolation { spec ->
            spec.classpath.from(ktLintClasspath, ruleSetsClasspath)
            spec.forkOptions { options ->
                options.maxHeapSize = workerMaxHeapSize.get()

                // Work around ktlint triggering reflective access errors from the embedded Kotlin
                // compiler. See https://youtrack.jetbrains.com/issue/KT-43704 for details.
                if (JavaVersion.current() >= JavaVersion.VERSION_16) {
                    options.jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
                }
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
            params.formatSnapshot.set(formatSnapshot)
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
        .getFileChanges(source)
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
