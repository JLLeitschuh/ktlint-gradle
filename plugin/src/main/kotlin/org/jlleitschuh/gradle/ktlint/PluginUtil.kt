package org.jlleitschuh.gradle.ktlint

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.api.BaseVariant
import java.nio.file.Path
import net.swiftzer.semver.SemVer
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.HelpTasksPlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin

internal fun resolveMainClassName(ktlintVersion: String) = when {
    SemVer.parse(ktlintVersion) < SemVer(0, 32, 0) -> "com.github.shyiko.ktlint.Main"
    else -> "com.pinterest.ktlint.Main"
}

internal inline fun <reified T : Task> Project.registerTask(
    name: String,
    noinline configuration: T.() -> Unit
): TaskProvider<T> {
    return this.tasks.register(name, T::class.java, configuration)
}

internal const val EDITOR_CONFIG_FILE_NAME = ".editorconfig"

internal fun getEditorConfigFiles(
    currentProject: Project,
    additionalEditorconfigFile: RegularFileProperty
): FileCollection {
    var editorConfigFileCollection = searchEditorConfigFiles(
        currentProject,
        currentProject.projectDir.toPath(),
        currentProject.files()
    )

    if (additionalEditorconfigFile.isPresent) {
        editorConfigFileCollection = editorConfigFileCollection.plus(
            currentProject.files(additionalEditorconfigFile.asFile.get().toPath())
        )
    }

    return editorConfigFileCollection
}

private tailrec fun searchEditorConfigFiles(
    project: Project,
    projectPath: Path,
    fileCollection: FileCollection
): FileCollection {
    val editorConfigFC = projectPath.resolve(EDITOR_CONFIG_FILE_NAME)
    val outputCollection = if (editorConfigFC.toFile().exists()) {
        fileCollection.plus(project.files(editorConfigFC))
    } else {
        fileCollection
    }

    val parentDir = projectPath.parent
    return if (parentDir != null &&
        projectPath != project.rootDir.toPath() &&
        !editorConfigFC.isRootEditorConfig()
    ) {
        searchEditorConfigFiles(project, parentDir, outputCollection)
    } else {
        outputCollection
    }
}

private val editorConfigRootRegex = "^root\\s?=\\s?true\\n".toRegex()

private fun Path.isRootEditorConfig(): Boolean {
    val asFile = toFile()
    if (!asFile.exists() || !asFile.canRead()) return false

    val reader = asFile.bufferedReader()
    var fileLine = reader.readLine()
    while (fileLine != null) {
        if (fileLine.contains(editorConfigRootRegex)) {
            return true
        }
        fileLine = reader.readLine()
    }

    return false
}

internal const val VERIFICATION_GROUP = LifecycleBasePlugin.VERIFICATION_GROUP
internal const val FORMATTING_GROUP = "Formatting"
internal const val HELP_GROUP = HelpTasksPlugin.HELP_GROUP
internal const val CHECK_PARENT_TASK_NAME = "ktlintCheck"
internal const val FORMAT_PARENT_TASK_NAME = "ktlintFormat"
internal const val APPLY_TO_IDEA_TASK_NAME = "ktlintApplyToIdea"
internal const val APPLY_TO_IDEA_GLOBALLY_TASK_NAME = "ktlintApplyToIdeaGlobally"
internal const val KOTLIN_SCRIPT_CHECK_TASK = "ktlintKotlinScriptCheck"
internal const val KOTLIN_SCRIPT_FORMAT_TASK = "ktlintKotlinScriptFormat"
internal const val INSTALL_GIT_HOOK_CHECK_TASK = "addKtlintCheckGitPreCommitHook"
internal const val INSTALL_GIT_HOOK_FORMAT_TASK = "addKtlintFormatGitPreCommitHook"
internal val KOTLIN_EXTENSIONS = listOf("kt", "kts")

internal inline fun <reified T> ObjectFactory.property(
    configuration: Property<T>.() -> Unit = {}
) = property(T::class.java).apply(configuration)

internal inline fun <reified T> ObjectFactory.setProperty(
    configuration: SetProperty<T>.() -> Unit = {}
) = setProperty(T::class.java).apply(configuration)

internal inline fun <reified T> ObjectFactory.listProperty(
    configuration: ListProperty<T>.() -> Unit = {}
) = listProperty(T::class.java).apply(configuration)

/**
 * Create check task name from source set name.
 */
internal fun String.sourceSetCheckTaskName() = "ktlint${capitalize()}SourceSetCheck"

/**
 * Create format task name from source set name.
 */
internal fun String.sourceSetFormatTaskName() = "ktlint${capitalize()}SourceSetFormat"

/**
 * Create check task name for android variant name with optional android multiplatform target name addition.
 */
internal fun String.androidVariantMetaCheckTaskName(
    multiplatformTargetName: String? = null
) = "ktlint${capitalize()}${multiplatformTargetName?.capitalize() ?: ""}Check"

/**
 * Create format task name for android variant name with optional android multiplatform target name addition.
 */
internal fun String.androidVariantMetaFormatTaskName(
    multiplatformTargetName: String? = null
) = "ktlint${capitalize()}${multiplatformTargetName?.capitalize() ?: ""}Format"

/**
 * Get specific android variants from [BaseExtension].
 */
internal val BaseExtension.variants: DomainObjectSet<out BaseVariant>?
    get() = when (this) {
        is AppExtension -> applicationVariants
        is LibraryExtension -> libraryVariants
        is FeatureExtension -> featureVariants
        is TestExtension -> applicationVariants
        else -> null // Instant app extension doesn't provide variants access
    }
