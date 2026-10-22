package org.jlleitschuh.gradle.ktlint.tasks

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.InputChanges
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.FORMAT_KOTLIN_SCRIPT_TASK_NAME
import org.jlleitschuh.gradle.ktlint.intermediateResultsBuildDir
import org.jlleitschuh.gradle.ktlint.worker.KtLintWorkAction
import org.jlleitschuh.gradle.ktlint.worker.KtLintWorkAction.FormatTaskSnapshot.Companion.contentHash
import javax.inject.Inject

@CacheableTask
abstract class KtLintFormatTask @Inject constructor(
    workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout
) : BaseKtLintCheckTask(workerExecutor, objectFactory, projectLayout) {

    @get:LocalState
    internal abstract val previousRunSnapshot: RegularFileProperty

    @TaskAction
    fun format(inputChanges: InputChanges) {
        runFormat(inputChanges, previousRunSnapshot.get().asFile)
    }

    internal companion object {
        fun buildTaskNameForSourceSet(
            sourceSetName: String
        ): String = "runKtlintFormatOver${sourceSetName.capitalize()}SourceSet"

        fun register(
            project: Project,
            sources: FileCollection,
            name: String
        ): TaskProvider<KtLintFormatTask> {
            return project.tasks.register(name, KtLintFormatTask::class.java) { t ->
                t.configureBaseCheckTask(project)
                t.setSource(sources)
                t.discoveredErrors.set(project.layout.intermediateResultsBuildDir("${name}_errors.bin"))
                t.workerMaxHeapSize.convention("256m")
                t.previousRunSnapshot.set(project.layout.intermediateResultsBuildDir("$name-snapshot.bin"))
                t.description = "Lints files to ensure that they are formatted according to the code style " +
                    "and, on error, tries to format code to conform code style."
                if (name != FORMAT_KOTLIN_SCRIPT_TASK_NAME) {
                    t.mustRunAfter(project.tasks.named(FORMAT_KOTLIN_SCRIPT_TASK_NAME))
                }
                // Special UP-TO-DATE check to avoid situation when task does not check restored to pre-formatted state
                // files
                t.outputs.upToDateWhen {
                    val inputSources = t.source.files
                    t.previousRunSnapshot.asFile.get()
                        .run {
                            if (exists()) {
                                KtLintWorkAction.FormatTaskSnapshot.readFromFile(this)
                            } else {
                                KtLintWorkAction.FormatTaskSnapshot(emptyMap())
                            }
                        }.formattedSources.none {
                            inputSources.contains(it.key) &&
                                contentHash(it.key).contentEquals(it.value)
                        }
                }
            }
        }
    }
}
