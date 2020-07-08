package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.reporter.CustomReporter

internal typealias FilterApplier = (Action<PatternFilterable>) -> Unit
internal typealias KotlinScriptAdditionalPathApplier = (ConfigurableFileTree) -> Unit

/**
 * The base Ktlint plugin that all other plugins are built on.
 */
open class KtlintBasePlugin : Plugin<Project> {
    internal lateinit var extension: KtlintExtension

    override fun apply(target: Project) {
        val filterTargetApplier: FilterApplier = {
            target.tasks.withType(BaseKtlintCheckTask::class.java).configureEach(it)
        }

        target.tasks.withType(BaseKtlintCheckTask::class.java).configureEach {
            val objects = target.objects
            val gradleVersion = GradleVersion.version(target.gradle.gradleVersion)
            if (gradleVersion < GradleVersion.version("6.0")) {
                it.runner = objects.newInstance(JavaExecKtLintRunner::class.java, target)
            } else {
                it.runner = objects.newInstance(WorkerApiKtLintRunner::class.java)
            }
        }

        val kotlinScriptAdditionalPathApplier: KotlinScriptAdditionalPathApplier = { additionalFileTree ->
            val configureAction = Action<Task> { task ->
                with(task as BaseKtlintCheckTask) {
                    source = source.plus(additionalFileTree.also {
                        it.include("*.kts")
                    })
                }
            }

            target.tasks.named(KOTLIN_SCRIPT_CHECK_TASK).configure(configureAction)
            target.tasks.named(KOTLIN_SCRIPT_FORMAT_TASK).configure(configureAction)
        }

        extension = target.extensions.create(
            "ktlint",
            KtlintExtension::class.java,
            target.objects,
            target.container(CustomReporter::class.java) { name -> CustomReporter(name, target.dependencies) },
            filterTargetApplier,
            kotlinScriptAdditionalPathApplier
        )
    }
}
