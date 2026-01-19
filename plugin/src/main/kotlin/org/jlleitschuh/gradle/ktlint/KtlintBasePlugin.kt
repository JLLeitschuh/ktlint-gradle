package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

internal typealias FilterApplier = (Action<PatternFilterable>) -> Unit
internal typealias KotlinScriptAdditionalPathApplier = (ConfigurableFileTree) -> Unit

/**
 * The base Ktlint plugin that all other plugins are built on.
 */
open class KtlintBasePlugin : Plugin<Project> {
    internal lateinit var extension: KtlintExtension

    override fun apply(target: Project) {
        val filterTargetApplier: FilterApplier = {
            target.tasks.withType(BaseKtLintCheckTask::class.java).configureEach(it)
        }

        val kotlinScriptAdditionalPathApplier: KotlinScriptAdditionalPathApplier = { additionalFileTree ->
            val configureAction = Action<Task> {
                with(this as BaseKtLintCheckTask) {
                    source(
                        additionalFileTree.also {
                            it.include("*.kts")
                        }
                    )
                }
            }

            target.tasks.named(KtLintCheckTask.KOTLIN_SCRIPT_TASK_NAME).configure(configureAction)
            target.tasks.named(KtLintFormatTask.KOTLIN_SCRIPT_TASK_NAME).configure(configureAction)
        }

        extension = target.extensions.create(
            "ktlint",
            KtlintExtension::class.java,
            target.objects,
            filterTargetApplier,
            kotlinScriptAdditionalPathApplier
        )
    }

    companion object {
        const val LOWEST_SUPPORTED_GRADLE_VERSION = "7.4.2"
    }

    /**
     * @deprecated Now that we declare gradle API metadata, this code should not be needed.
     * Ee need to check which version of gradle introduced gradle API metadata checking
     */
    @Deprecated("Now that we declare gradle API metadata, this code should not be needed")
    private fun Project.checkMinimalSupportedGradleVersion() {
        if (GradleVersion.version(gradle.gradleVersion) < GradleVersion.version(LOWEST_SUPPORTED_GRADLE_VERSION)) {
            throw GradleException(
                "Current version of plugin supports minimal Gradle version: $LOWEST_SUPPORTED_GRADLE_VERSION"
            )
        }
    }
}
