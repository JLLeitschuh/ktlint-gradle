package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The base Ktlint plugin that all other plugins are built on.
 */
open class KtlintBasePlugin : Plugin<Project> {
    internal lateinit var extension: KtlintExtension

    override fun apply(target: Project) {
        extension = target.extensions.create("ktlint", KtlintExtension::class.java, target)
    }
}
