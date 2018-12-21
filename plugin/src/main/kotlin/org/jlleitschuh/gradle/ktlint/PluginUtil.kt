package org.jlleitschuh.gradle.ktlint

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.api.BaseVariant
import net.swiftzer.semver.SemVer
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.HelpTasksPlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.nio.file.Path

internal fun createConfiguration(target: Project, extension: KtlintExtension) =
    target.configurations.maybeCreate("ktlint").apply {
        target.dependencies.add(
            this.name,
            mapOf(
                "group" to "com.github.shyiko",
                "name" to "ktlint",
                "version" to extension.version.get()
            )
        )
    }

internal inline fun <reified T : Task> Project.registerTask(
    name: String,
    noinline configuration: T.() -> Unit
): TaskProvider<T> {
    return this.tasks.register(name, T::class.java, configuration)
}

internal const val EDITOR_CONFIG_FILE_NAME = ".editorconfig"

internal fun getEditorConfigFiles(currentProject: Project): FileCollection {
    return searchEditorConfigFiles(
        currentProject,
        currentProject.projectDir.toPath(),
        currentProject.files()
    )
}

private tailrec fun searchEditorConfigFiles(
    project: Project,
    projectPath: Path,
    fileCollection: FileCollection
): FileCollection {
    val editorConfigFC = projectPath.resolve(EDITOR_CONFIG_FILE_NAME)
    val outputCollection = if (editorConfigFC != null) {
        fileCollection.plus(project.files(editorConfigFC))
    } else {
        fileCollection
    }

    val parentDir = projectPath.parent
    return if (parentDir != null &&
        parentDir != project.rootDir.toPath() &&
        !editorConfigFC.isRootEditorConfig()) {
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

/**
 * Android option is available from ktlint 0.12.0.
 */
internal fun Property<String>.isAndroidFlagAvailable() =
    SemVer.parse(get()) >= SemVer(0, 12, 0)

internal const val VERIFICATION_GROUP = LifecycleBasePlugin.VERIFICATION_GROUP
internal const val FORMATTING_GROUP = "Formatting"
internal const val HELP_GROUP = HelpTasksPlugin.HELP_GROUP
internal const val CHECK_PARENT_TASK_NAME = "ktlintCheck"
internal const val FORMAT_PARENT_TASK_NAME = "ktlintFormat"
internal const val APPLY_TO_IDEA_TASK_NAME = "ktlintApplyToIdea"
internal const val APPLY_TO_IDEA_GLOBALLY_TASK_NAME = "ktlintApplyToIdeaGlobally"
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
internal val BaseExtension.variants: DomainObjectSet<out BaseVariant>? get() = when (this) {
    is AppExtension -> applicationVariants
    is LibraryExtension -> libraryVariants
    is FeatureExtension -> featureVariants
    is TestExtension -> applicationVariants
    else -> null // Instant app extension doesn't provide variants access
}