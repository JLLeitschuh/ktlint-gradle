package org.jlleitschuh.gradle.ktlint.android

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
import org.gradle.api.Plugin
import org.gradle.api.file.FileCollection
import org.jlleitschuh.gradle.ktlint.KtlintPlugin
import org.jlleitschuh.gradle.ktlint.addGenerateReportsTaskToProjectMetaCheckTask
import org.jlleitschuh.gradle.ktlint.addGenerateReportsTaskToProjectMetaFormatTask
import org.jlleitschuh.gradle.ktlint.createCheckTask
import org.jlleitschuh.gradle.ktlint.createFormatTask
import org.jlleitschuh.gradle.ktlint.createGenerateReportsTask
import org.jlleitschuh.gradle.ktlint.setCheckTaskDependsOnGenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import java.util.concurrent.Callable

internal fun KtlintPlugin.PluginHolder.applyKtLintToAndroid(): (Plugin<in Any>) -> Unit {
    return {
        target.plugins.withId(
            "com.android.application",
            androidPluginConfigureAction(this)
        )
        target.plugins.withId(
            "com.android.library",
            androidPluginConfigureAction(this)
        )
        target.plugins.withId(
            "com.android.test",
            androidPluginConfigureAction(this)
        )
        target.plugins.withId(
            "com.android.dynamic-feature",
            androidPluginConfigureAction(this)
        )
    }
}

/*
 * Variant manager returns all sources for variant,
 * so most probably main source set maybe checked several times.
 * This approach creates one check tasks per one source set.
 */
@Suppress("UNCHECKED_CAST", "UnstableApiUsage")
private fun androidPluginConfigureAction(
    pluginHolder: KtlintPlugin.PluginHolder
): (Plugin<Any>) -> Unit = {
    pluginHolder.target.extensions.configure<BaseExtension>("android") { android ->
        android.sourceSets.configureEach { sourceSet ->
            val srcDirs = sourceSet.java.srcDirs +
                (sourceSet.kotlin as? DefaultAndroidSourceDirectorySet)?.srcDirs.orEmpty()
            pluginHolder.createAndroidTasks(
                sourceSet.name,
                pluginHolder.target.files(Callable { srcDirs })
            )
        }
    }
}

private fun KtlintPlugin.PluginHolder.createAndroidTasks(
    sourceSetName: String,
    sources: FileCollection
) {
    val checkTask = createCheckTask(
        this,
        sourceSetName,
        sources
    )
    val generateReportsCheckTask = createGenerateReportsTask(
        this,
        checkTask,
        GenerateReportsTask.LintType.CHECK,
        sourceSetName
    )

    addGenerateReportsTaskToProjectMetaCheckTask(generateReportsCheckTask)
    setCheckTaskDependsOnGenerateReportsTask(generateReportsCheckTask)

    val formatTask = createFormatTask(
        this,
        sourceSetName,
        sources
    )
    val generateReportsFormatTask = createGenerateReportsTask(
        this,
        formatTask,
        GenerateReportsTask.LintType.FORMAT,
        sourceSetName
    )
    addGenerateReportsTaskToProjectMetaFormatTask(generateReportsFormatTask)
}
