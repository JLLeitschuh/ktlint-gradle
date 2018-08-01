package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.HelpTasksPlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin

internal fun createConfiguration(target: Project, extension: KtlintExtension) =
    target.configurations.maybeCreate("ktlint").apply {
        target.dependencies.add(
            this.name,
            mapOf(
                "group" to "com.github.shyiko",
                "name" to "ktlint",
                "version" to extension.version
            )
        )
    }

internal inline fun <reified T : Task> Project.taskHelper(name: String, noinline configuration: T.() -> Unit): T {
    return this.tasks.create(name, T::class.java, configuration)
}

/**
 * Android option is available from ktlint 0.12.0.
 */
internal fun KtlintExtension.isAndroidFlagEnabled() =
    android && SemVer.parse(version) >= SemVer(0, 12, 0)

internal const val VERIFICATION_GROUP = LifecycleBasePlugin.VERIFICATION_GROUP
internal const val FORMATTING_GROUP = "Formatting"
internal const val HELP_GROUP = HelpTasksPlugin.HELP_GROUP
internal const val CHECK_PARENT_TASK_NAME = "ktlintCheck"
internal const val FORMAT_PARENT_TASK_NAME = "ktlintFormat"
internal const val APPLY_TO_IDEA_TASK_NAME = "ktlintApplyToIdea"
internal const val APPLY_TO_IDEA_GLOBALLY_TASK_NAME = "ktlintApplyToIdeaGlobally"
internal val KOTLIN_EXTENSIONS = listOf("kt", "kts")