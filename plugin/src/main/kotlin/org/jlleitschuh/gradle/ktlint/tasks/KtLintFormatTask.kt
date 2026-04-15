package org.jlleitschuh.gradle.ktlint.tasks

import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.intermediateResultsBuildDir
import org.jlleitschuh.gradle.ktlint.worker.KtLintWorkAction
import org.jlleitschuh.gradle.ktlint.worker.KtLintWorkAction.FormatTaskSnapshot.Companion.contentHash
import javax.inject.Inject

@CacheableTask
abstract class KtLintFormatTask @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    workerExecutor: WorkerExecutor,
    patternFilterable: PatternFilterable
) : BaseKtLintCheckTask(
    objectFactory,
    projectLayout,
    workerExecutor,
    patternFilterable
) {
    @get:LocalState
    internal val previousRunSnapshot: RegularFileProperty = objectFactory
        .fileProperty()
        .convention(
            projectLayout.intermediateResultsBuildDir("$name-snapshot.bin")
        )

    private val previousSnapshot get() = previousRunSnapshot.asFile.get()
        .run {
            if (exists()) {
                KtLintWorkAction.FormatTaskSnapshot.readFromFile(this)
            } else {
                KtLintWorkAction.FormatTaskSnapshot(emptyMap())
            }
        }

    init {
        // Special UP-TO-DATE check to avoid situation when task does not check restored to pre-formatted state
        // files
        outputs.upToDateWhen {
            val inputSources = source.files
            previousSnapshot.formattedSources.none {
                inputSources.contains(it.key) &&
                    contentHash(it.key).contentEquals(it.value)
            }
        }
    }

    @TaskAction
    fun format(inputChanges: InputChanges) {
        runFormat(inputChanges, previousRunSnapshot.get().asFile)
    }

    internal companion object {
        fun buildTaskNameForSourceSet(
            sourceSetName: String
        ): String = "runKtlintFormatOver${sourceSetName.replaceFirstChar { it.uppercase() }}SourceSet"

        const val KOTLIN_SCRIPT_TASK_NAME = "runKtlintFormatOverKotlinScripts"

        fun buildDescription(
            fileType: String
        ): String = "Lints all $fileType files to ensure that they are formatted according to the code style " +
            " and, on error, tries to format code to conform code style."
    }
}
