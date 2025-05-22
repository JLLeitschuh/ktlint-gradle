package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.HelpTasksPlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

internal inline fun <reified T : Task> Project.registerTask(
    name: String,
    vararg constructorArguments: Any = emptyArray(),
    noinline configuration: T.() -> Unit
): TaskProvider<T> {
    return tasks
        .register<T>(name, *constructorArguments)
        .apply {
            this.configure(configuration)
        }
}

internal const val EDITOR_CONFIG_FILE_NAME = ".editorconfig"

internal fun getEditorConfigFiles(currentProjectDir: Path): Set<Path> {
    val result = mutableSetOf<Path>()
    searchEditorConfigFiles(
        currentProjectDir,
        result
    )
    return result
}

private tailrec fun searchEditorConfigFiles(
    projectPath: Path,
    result: MutableSet<Path>
) {
    val editorConfigFC = projectPath.resolve(EDITOR_CONFIG_FILE_NAME)
    if (Files.exists(editorConfigFC)) {
        result.add(editorConfigFC.toAbsolutePath())
    }

    val parentDir = projectPath.parent
    if (parentDir != null &&
        !editorConfigFC.isRootEditorConfig()
    ) {
        searchEditorConfigFiles(parentDir, result)
    }
}

private val editorConfigRootRegex = "^root\\s?=\\s?true".toRegex()

internal fun Path.isRootEditorConfig(): Boolean {
    if (!Files.exists(this) || !Files.isReadable(this)) return false

    toFile().useLines { lines ->
        val isRoot = lines.firstOrNull { it.contains(editorConfigRootRegex) }
        return@isRootEditorConfig isRoot != null
    }
}

internal const val VERIFICATION_GROUP = LifecycleBasePlugin.VERIFICATION_GROUP
internal const val FORMATTING_GROUP = "Formatting"
internal const val HELP_GROUP = HelpTasksPlugin.HELP_GROUP
internal const val CHECK_PARENT_TASK_NAME = "ktlintCheck"
internal const val FORMAT_PARENT_TASK_NAME = "ktlintFormat"
internal const val INSTALL_GIT_HOOK_CHECK_TASK = "addKtlintCheckGitPreCommitHook"
internal const val INSTALL_GIT_HOOK_FORMAT_TASK = "addKtlintFormatGitPreCommitHook"
internal val KOTLIN_EXTENSIONS = listOf("kt", "kts")
internal val INTERMEDIATE_RESULTS_PATH = "intermediates${File.separator}ktLint${File.separator}"

internal inline fun <reified T> ObjectFactory.property(
    configuration: Property<T>.() -> Unit = {}
): Property<T> = property(T::class.java).apply(configuration)

internal inline fun <reified T> ObjectFactory.setProperty(
    configuration: SetProperty<T>.() -> Unit = {}
): SetProperty<T> = setProperty(T::class.java).apply(configuration)

internal fun Project.isConsolePlain(): Boolean = gradle.startParameter.consoleOutput == ConsoleOutput.Plain

/**
 * Get file path where tasks could put their intermediate results, that could be consumed by other plugin tasks.
 */
internal fun ProjectLayout.intermediateResultsBuildDir(
    resultsFile: String
): Provider<RegularFile> = buildDirectory.file("$INTERMEDIATE_RESULTS_PATH$resultsFile")

/**
 * Logs into Gradle console KtLint debug message.
 */
internal fun Logger.logKtLintDebugMessage(
    debugIsEnabled: Boolean,
    logProducer: () -> List<String>
) {
    if (debugIsEnabled) {
        logProducer().forEach {
            warn("[KtLint DEBUG] $it")
        }
    }
}

internal fun checkMinimalSupportedKtLintVersion(ktLintVersion: String) {
    if (SemVer.parse(ktLintVersion) < SemVer(0, 47, 1)) {
        throw GradleException(
            "KtLint versions less than 0.47.1 are not supported. " +
                "Detected KtLint version: $ktLintVersion."
        )
    }
}
