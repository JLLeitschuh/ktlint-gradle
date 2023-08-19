package org.jlleitschuh.gradle.ktlint.android

import com.android.build.api.dsl.BuildFeatures
import com.android.build.api.dsl.BuildType
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DefaultConfig
import com.android.build.api.dsl.ProductFlavor
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

private typealias AndroidCommonExtension =
    CommonExtension<BuildFeatures, BuildType, DefaultConfig, ProductFlavor>

/*
 * Variant manager returns all sources for variant,
 * so most probably main source set maybe checked several times.
 * This approach creates one check tasks per one source set.
 */
@Suppress("UNCHECKED_CAST", "UnstableApiUsage")
private fun androidPluginConfigureAction(
    pluginHolder: KtlintPlugin.PluginHolder
): (Plugin<Any>) -> Unit = {
    pluginHolder.target.extensions.configure(CommonExtension::class.java) {
        val androidCommonExtension = this as AndroidCommonExtension

        androidCommonExtension.sourceSets.all {
            // https://issuetracker.google.com/u/1/issues/170650362
            val androidSourceSet = kotlin as DefaultAndroidSourceDirectorySet
            // Passing Callable, so returned FileCollection, will lazy evaluate it
            // only when task will need it.
            // Solves the problem of having additional source dirs in
            // current AndroidSourceSet, that are not available on eager
            // evaluation.
            pluginHolder.createAndroidTasks(
                name,
                pluginHolder.target.files(Callable { androidSourceSet.srcDirs })
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
