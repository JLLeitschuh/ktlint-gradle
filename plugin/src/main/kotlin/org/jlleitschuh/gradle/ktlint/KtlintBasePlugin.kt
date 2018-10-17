package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.util.GradleVersion

typealias FilterApplier = (Action<PatternFilterable>) -> Unit

/**
 * The base Ktlint plugin that all other plugins are built on.
 */
open class KtlintBasePlugin : Plugin<Project> {
    internal lateinit var extension: KtlintExtension

    override fun apply(target: Project) {
        val filterTargetApplier: FilterApplier = {
            if (GradleVersion.current() >= GradleVersion.version("4.9")) {
                // API only added in Gradle 4.9
                target.tasks.withType(KtlintCheckTask::class.java).configureEach(it)
            } else {
                target.tasks.withType(KtlintCheckTask::class.java, it)
            }
        }

        extension = target.extensions.create(
            "ktlint",
            KtlintExtension::class.java,
            target.objects,
            filterTargetApplier
        )
    }
}
