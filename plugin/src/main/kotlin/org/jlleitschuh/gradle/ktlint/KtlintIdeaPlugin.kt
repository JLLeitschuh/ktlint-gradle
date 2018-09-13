package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Adds tasks associated with configuring IntelliJ IDEA.
 */
open class KtlintIdeaPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.plugins.apply(KtlintBasePlugin::class.java).extension

        if (target == target.rootProject) {
            /*
             * Only add these tasks if we are applying to the root project.
             */
            addApplyToIdeaTasks(target, extension)
        }
    }

    private fun addApplyToIdeaTasks(rootProject: Project, extension: KtlintExtension) {
        val ktLintConfig = createConfiguration(rootProject, extension)

        rootProject.taskHelper<KtlintApplyToIdeaTask>(APPLY_TO_IDEA_TASK_NAME) {
            group = HELP_GROUP
            description = "Generates IDEA built-in formatter rules and apply them to the project." +
                "It will overwrite existing ones."
            classpath.setFrom(ktLintConfig)
            android.set(rootProject.provider { extension.isAndroidFlagEnabled() })
            globally.set(rootProject.provider { false })
            ktlintVersion.set(extension.version)
        }

        rootProject.taskHelper<KtlintApplyToIdeaTask>(APPLY_TO_IDEA_GLOBALLY_TASK_NAME) {
            group = HELP_GROUP
            description = "Generates IDEA built-in formatter rules and apply them globally " +
                "(in IDEA user settings folder). It will overwrite existing ones."
            classpath.setFrom(ktLintConfig)
            android.set(rootProject.provider { extension.isAndroidFlagEnabled() })
            globally.set(rootProject.provider { true })
            ktlintVersion.set(extension.version)
        }
    }
}