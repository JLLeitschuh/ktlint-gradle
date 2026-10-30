package org.jlleitschuh.gradle.ktlint.android

import com.android.build.api.dsl.AndroidSourceDirectorySet
import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jlleitschuh.gradle.ktlint.addGenerateReportsTaskToProjectMetaCheckTask
import org.jlleitschuh.gradle.ktlint.addGenerateReportsTaskToProjectMetaFormatTask
import org.jlleitschuh.gradle.ktlint.setCheckTaskDependsOnGenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import kotlin.reflect.full.memberProperties

internal fun applyKtLintToAndroid(project: Project): (Plugin<in Any>) -> Unit {
    return {
        project.plugins.withId(
            "com.android.application",
            androidPluginConfigureAction(project)
        )
        project.plugins.withId(
            "com.android.library",
            androidPluginConfigureAction(project)
        )
        project.plugins.withId(
            "com.android.test",
            androidPluginConfigureAction(project)
        )
        project.plugins.withId(
            "com.android.dynamic-feature",
            androidPluginConfigureAction(project)
        )
    }
}

/*
 * Variant manager returns all sources for variant,
 * so most probably main source set maybe checked several times.
 * This approach creates one check tasks per one source set.
 */
@Suppress("UnstableApiUsage")
private fun androidPluginConfigureAction(project: Project): (Plugin<Any>) -> Unit = {
    project.extensions.configure(CommonExtension::class.java) {
        // kotlin property exists in AGP >= 7
        val kotlinProperty = AndroidSourceSet::class.memberProperties.firstOrNull { it.name == "kotlin" }
        if (kotlinProperty == null) {
            project.logger.warn(
                buildString {
                    append("In AGP <7 kotlin source directories are not auto-detected. ")
                    append("In order to lint kotlin sources, manually add the directory to the source set. ")
                    append("""For example: sourceSets.getByName("main").java.srcDirs("src/main/kotlin/")""")
                }
            )
        }
        val sourceMember: AndroidSourceSet.() -> AndroidSourceDirectorySet = {
            kotlinProperty?.get(this) as AndroidSourceDirectorySet? ?: this.java
        }
        sourceSets.all {
            val androidSourceSet = sourceMember(this) as DefaultAndroidSourceDirectorySet
            // Passing Callable, so returned FileCollection, will lazy evaluate it
            // only when task will need it.
            // Solves the problem of having additional source dirs in
            // current AndroidSourceSet, that are not available on eager
            // evaluation.
            createAndroidTasks(project, name, project.files(androidSourceSet.srcDirs))
        }
    }
}

private fun createAndroidTasks(
    project: Project,
    sourceSetName: String,
    sources: FileCollection
) {
    val checkTask =
        KtLintCheckTask.register(project, sources, KtLintCheckTask.buildTaskNameForSourceSet(sourceSetName))
    val generateReportsCheckTask =
        GenerateReportsTask.registerCheckReportTaskForSourceSet(project, checkTask, sourceSetName)

    addGenerateReportsTaskToProjectMetaCheckTask(project, generateReportsCheckTask)
    setCheckTaskDependsOnGenerateReportsTask(project, generateReportsCheckTask)

    val formatTask =
        KtLintFormatTask.register(project, sources, KtLintFormatTask.buildTaskNameForSourceSet(sourceSetName))
    val generateReportsFormatTask =
        GenerateReportsTask.registerFormatReportTaskForSourceSet(project, formatTask, sourceSetName)
    addGenerateReportsTaskToProjectMetaFormatTask(project, generateReportsFormatTask)
}
