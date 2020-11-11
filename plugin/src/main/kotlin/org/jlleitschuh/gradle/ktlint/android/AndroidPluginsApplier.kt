@file:Suppress("UnstableApiUsage")
package org.jlleitschuh.gradle.ktlint.android

import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.dsl.BuildFeatures
import com.android.build.api.dsl.BuildType
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DefaultConfig
import com.android.build.api.dsl.ProductFlavor
import com.android.build.api.dsl.SigningConfig
import com.android.build.api.variant.Variant
import com.android.build.api.variant.VariantProperties
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
import org.gradle.api.Plugin
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.jlleitschuh.gradle.ktlint.KtlintCheckTask
import org.jlleitschuh.gradle.ktlint.KtlintFormatTask
import org.jlleitschuh.gradle.ktlint.KtlintPlugin
import org.jlleitschuh.gradle.ktlint.addKtlintCheckTaskToProjectMetaCheckTask
import org.jlleitschuh.gradle.ktlint.addKtlintFormatTaskToProjectMetaFormatTask
import org.jlleitschuh.gradle.ktlint.createCheckTask
import org.jlleitschuh.gradle.ktlint.createFormatTask
import org.jlleitschuh.gradle.ktlint.setCheckTaskDependsOnKtlintCheckTask
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

private typealias AndroidCommonExtension = CommonExtension<
    AndroidSourceSet,
    BuildFeatures,
    BuildType,
    DefaultConfig,
    ProductFlavor,
    SigningConfig,
    Variant<VariantProperties>,
    VariantProperties
    >

/*
 * Variant manager returns all sources for variant,
 * so most probably main source set maybe checked several times.
 * This approach creates one check tasks per one source set.
 */
@Suppress("UNCHECKED_CAST")
private fun androidPluginConfigureAction(
    pluginHolder: KtlintPlugin.PluginHolder
): (Plugin<Any>) -> Unit = {
    pluginHolder.target.extensions.configure(CommonExtension::class.java) { ext ->
        val androidCommonExtension = ext as AndroidCommonExtension

        androidCommonExtension.sourceSets.all { sourceSet ->
            // https://issuetracker.google.com/u/1/issues/170650362
            val androidSourceSet = sourceSet.java as DefaultAndroidSourceDirectorySet
            // Passing Callable, so returned FileCollection, will lazy evaluate it
            // only when task will need it.
            // Solves the problem of having additional source dirs in
            // current AndroidSourceSet, that are not available on eager
            // evaluation.
            pluginHolder.createAndroidTasks(
                sourceSet.name,
                pluginHolder.target.files(Callable { androidSourceSet.srcDirs })
            )
        }
    }
}

private fun KtlintPlugin.PluginHolder.createAndroidTasks(
    sourceSetName: String,
    sources: FileCollection
): Pair<TaskProvider<KtlintCheckTask>, TaskProvider<KtlintFormatTask>> {
    val checkTask = createCheckTask(
        this,
        sourceSetName,
        sources
    )

    addKtlintCheckTaskToProjectMetaCheckTask(checkTask)
    setCheckTaskDependsOnKtlintCheckTask(target, checkTask)

    val formatTask = createFormatTask(
        this,
        sourceSetName,
        sources
    )

    addKtlintFormatTaskToProjectMetaFormatTask(formatTask)
    return checkTask to formatTask
}
