package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.util.PatternFilterable

/**
 * The base Ktlint plugin that all other plugins are built on.
 */
open class KtlintBasePlugin : Plugin<Project> {
    internal lateinit var extension: KtlintExtension

    override fun apply(target: Project) {
        val filterTargetApplier = fun(action: Action<PatternFilterable>) {
            target.tasks.withType(KtlintCheckTask::class.java).configureEach(action)
        }
        extension = target.extensions.create(
            "ktlint",
            KtlintExtension::class.java,
            target.objects,
            filterTargetApplier
        )
    }
}
