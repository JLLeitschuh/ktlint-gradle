package org.jlleitschuh.gradle.ktlint.tasks

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTreeElement
import org.gradle.api.file.FileType
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.CacheableTask
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
import org.gradle.api.tasks.util.PatternSet
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.FILTER_INCLUDE_PROPERTY_NAME
import org.jlleitschuh.gradle.ktlint.KOTLIN_EXTENSIONS
import org.jlleitschuh.gradle.ktlint.KTLINT_CONFIGURATION_NAME
import org.jlleitschuh.gradle.ktlint.KTLINT_RULESET_CONFIGURATION_NAME
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.applyGitFilter
import org.jlleitschuh.gradle.ktlint.getEditorConfigFiles
import org.jlleitschuh.gradle.ktlint.worker.KtLintClassesSerializer
import org.jlleitschuh.gradle.ktlint.worker.KtLintWorkAction
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class BaseKtLintCheckTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout
) : DefaultTask(), PatternFilterable {
    private val patternFilterable: PatternFilterable = PatternSet()
    private val sourceFiles: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Classpath
    internal abstract val ktLintClasspath: ConfigurableFileCollection

    @get:Input
    internal abstract val additionalEditorconfig: MapProperty<String, String>

    @get:Incremental
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    internal val editorConfigFiles: FileCollection = objectFactory.fileCollection().from(
        {
            getEditorConfigFiles(projectLayout.projectDirectory.asFile.toPath())
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
    internal abstract val enableExperimentalRules: Property<Boolean>

    /**
     * Max lint worker heap size. Default is "256m".
     */
    @get:Internal
    abstract val workerMaxHeapSize: Property<String>

    init {
        if (project.providers.gradleProperty(FILTER_INCLUDE_PROPERTY_NAME).orNull != null) {
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

    fun source(vararg sources: Any?) = also { sourceFiles.setFrom(*sources) }

    fun setSource(source: Any) {
        sourceFiles.setFrom(source)
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    internal abstract val loadedReporters: RegularFileProperty

    @get:OutputFile
    internal abstract val discoveredErrors: RegularFileProperty

    protected fun runLint(
        inputChanges: InputChanges
    ) {
        val editorConfigUpdated = wasEditorConfigFilesUpdated(inputChanges)
        val filesToCheck = if (editorConfigUpdated) {
            source.files
        } else {
            getChangedSources(inputChanges)
        }

        logTaskExecutionState(inputChanges, editorConfigUpdated)
        if (skipExecution(filesToCheck)) return

        val previousErrors: Set<File> = if (discoveredErrors.asFile.get().exists()) {
            KtLintClassesSerializer
                .create()
                .loadErrors(discoveredErrors.asFile.get())
                .map { it.lintedFile }
                .filter { it.exists() }
                .toSet()
        } else {
            emptySet()
        }
        val allfilesToCheck = filesToCheck + previousErrors

        submitKtLintWork(allfilesToCheck, false, editorConfigUpdated, null)
    }

    protected fun runFormat(
        inputChanges: InputChanges,
        formatSnapshot: File
    ) {
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
        editorConfigUpdated: Boolean
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
        val queue = workerExecutor.processIsolation {
            classpath.from(ktLintClasspath, ruleSetsClasspath)
            forkOptions {
                maxHeapSize = workerMaxHeapSize.get()

                // Work around ktlint triggering reflective access errors from the embedded Kotlin
                // compiler. See https://youtrack.jetbrains.com/issue/KT-43704 for details.
                if (JavaVersion.current() >= JavaVersion.VERSION_16) {
                    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
                }
            }
        }

        queue.submit(KtLintWorkAction::class.java) {
            val task = this@BaseKtLintCheckTask
            filesToLint.from(filesToCheck)
            android.set(task.android)
            debug.set(task.debug)
            additionalEditorconfig.set(task.additionalEditorconfig)
            formatSource.set(formatSources)
            discoveredErrorsFile.set(task.discoveredErrors)
            ktLintVersion.set(task.ktLintVersion)
            editorconfigFilesWereChanged.set(editorConfigUpdated)
            this.formatSnapshot.set(formatSnapshot)
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

    fun configureBaseCheckTask(project: Project) {
        val extension = project.extensions.getByType(KtlintExtension::class.java)
        val ktlintConfiguration = project.configurations.named(KTLINT_CONFIGURATION_NAME).get()
        val ktlintRulesetConfiguration = project.configurations.named(KTLINT_RULESET_CONFIGURATION_NAME).get()
        val loadReportersTask = project.tasks.named(LoadReportersTask.TASK_NAME, LoadReportersTask::class.java)
        ktLintClasspath.setFrom(ktlintConfiguration)
        ktLintVersion.set(extension.version)
        additionalEditorconfig.set(extension.additionalEditorconfig)
        debug.set(extension.debug)
        ruleSetsClasspath.setFrom(ktlintRulesetConfiguration)
        android.set(extension.android)
        loadedReporters.set(loadReportersTask.flatMap { it.loadedReporters })
        enableExperimentalRules.set(extension.enableExperimentalRules)
    }

    @Internal
    override fun getIncludes(): Set<String> = patternFilterable.includes

    @Internal
    override fun getExcludes(): Set<String> = patternFilterable.excludes
    override fun setIncludes(includes: MutableIterable<String>) = patternFilterable.setIncludes(includes)
    override fun setExcludes(excludes: MutableIterable<String>) = patternFilterable.setExcludes(excludes)
    override fun include(vararg includes: String) = patternFilterable.include(*includes)
    override fun include(includes: MutableIterable<String>) = patternFilterable.include(includes)
    override fun include(includeSpec: Spec<FileTreeElement>): PatternFilterable = patternFilterable.include(includeSpec)
    override fun include(includeSpec: Closure<*>) = patternFilterable.include(includeSpec)
    override fun exclude(vararg excludes: String): PatternFilterable = patternFilterable.exclude(*excludes)
    override fun exclude(excludes: MutableIterable<String>) = patternFilterable.exclude(excludes)
    override fun exclude(excludeSpec: Spec<FileTreeElement>) = patternFilterable.exclude(excludeSpec)
    override fun exclude(excludeSpec: Closure<*>) = patternFilterable.exclude(excludeSpec)
}
