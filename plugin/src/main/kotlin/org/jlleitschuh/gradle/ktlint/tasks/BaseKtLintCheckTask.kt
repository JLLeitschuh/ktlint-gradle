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
import org.jlleitschuh.gradle.ktlint.property
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
        filesToCheck: Set<File>,
        formatSources: Boolean,
    ) {
        checkDisabledRulesSupportedKtLintVersion()

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
        }
    }

    private fun checkDisabledRulesSupportedKtLintVersion() {
        if (disabledRules.get().isNotEmpty() &&
            SemVer.parse(ktLintVersion.get()) < SemVer(0, 34, 2)
        ) {
            throw GradleException("Rules disabling is supported since 0.34.2 ktlint version.")
        }
    }
}
