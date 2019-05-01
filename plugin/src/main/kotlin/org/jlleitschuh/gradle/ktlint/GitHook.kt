package org.jlleitschuh.gradle.ktlint

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.intellij.lang.annotations.Language
import java.io.File
import javax.inject.Inject

internal const val FILTER_INCLUDE_PROPERTY_NAME = "internalKtlintGitFilter"

@Language("Bash")
internal val shShebang = """
#!/bin/sh
set -e

""".trimIndent()

internal const val startHookSection = "######## KTLINT-GRADLE HOOK START ########\n"
internal const val endHookSection = "######## KTLINT-GRADLE HOOK END ########\n"

@Language("Bash")
internal fun generateGitHook(taskName: String) = """

CHANGED_FILES="${'$'}(git --no-pager diff --name-status --no-color --cached | awk '$1 != "D" && $2 ~ /\.kts|\.kt/ { print $2}')"

if [[ -z "${'$'}CHANGED_FILES" ]]; then
    echo "No Kotlin staged files."
    exit 0
fi;

echo "Running ktlint over these files:"
echo "${'$'}CHANGED_FILES"

./gradlew --quiet $taskName -P$FILTER_INCLUDE_PROPERTY_NAME="${'$'}CHANGED_FILES"

echo "Completed ktlint run."

while read -r file; do
    if [[ -f ${'$'}file ]]; then
        git add ${'$'}file
    fi
done <<< "${'$'}CHANGED_FILES"

echo "Completed ktlint hook."

""".trimIndent()

internal fun KtlintPlugin.PluginHolder.addGitHookTasks() {
    if (target.rootProject == target &&
        target.checkGitIsPresent()) {
        addInstallGitHookFormatTask()
        addInstallGitHookCheckTask()
    }
}

private val Project.gitFolder: File get() = rootProject.file(".git")
private fun Project.checkGitIsPresent(): Boolean = gitFolder.exists()

private fun KtlintPlugin.PluginHolder.addInstallGitHookFormatTask() {
    target.tasks.register(
        INSTALL_GIT_HOOK_FORMAT_TASK,
        KtlintInstallGitHookTask::class.java
    ) {
        it.description = "Adds git hook to run ktlintFormat on changed files"
        it.group = HELP_GROUP
        it.taskName.set(FORMAT_PARENT_TASK_NAME)
        it.gitHook.set(target.file(".git/hooks/pre-commit"))
    }
}

private fun KtlintPlugin.PluginHolder.addInstallGitHookCheckTask() {
    target.tasks.register(
        INSTALL_GIT_HOOK_CHECK_TASK,
        KtlintInstallGitHookTask::class.java
    ) {
        it.description = "Adds git hook to run ktlintCheck on changed files"
        it.group = HELP_GROUP
        it.taskName.set(CHECK_PARENT_TASK_NAME)
        it.gitHook.set(target.file(".git/hooks/pre-commit"))
    }
}

open class KtlintInstallGitHookTask @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout
) : DefaultTask() {
    @get:Input
    internal val taskName: Property<String> = objectFactory.property(String::class.java)

    @get:OutputFile
    internal val gitHook: RegularFileProperty = newFileProperty(objectFactory, projectLayout)

    @TaskAction
    fun installHook() {
        val gitHookFile = gitHook.get().asFile
        logger.info("Hook file: $gitHookFile")
        if (!gitHookFile.exists()) {
            gitHookFile.createNewFile()
            gitHookFile.setExecutable(true)
        }

        if (gitHookFile.length() == 0L) {
            gitHookFile.writeText(
                "$shShebang$startHookSection${generateGitHook(taskName.get())}$endHookSection"
            )
            return
        }

        var hookContent = gitHookFile.readText()
        if (hookContent.contains(startHookSection)) {
            val startTagIndex = hookContent.indexOf(startHookSection)
            val endTagIndex = hookContent.indexOf(endHookSection)
            hookContent = hookContent.replaceRange(
                startTagIndex,
                endTagIndex,
                "$startHookSection${generateGitHook(taskName.get())}"
            )
            gitHookFile.writeText(hookContent)
        } else {
            gitHookFile.appendText(
                "$startHookSection${generateGitHook(taskName.get())}$endHookSection"
            )
        }
    }
}

internal fun KtlintCheckTask.applyGitFilter() {
    val projectRelativePath = project.rootDir.toPath()
        .relativize(project.projectDir.toPath())
        .toString()
    val filesToInclude = (project.property(FILTER_INCLUDE_PROPERTY_NAME) as String)
        .split('\n')
        .filter { it.startsWith(projectRelativePath) }

    if (filesToInclude.isNotEmpty()) {
        include { fileTreeElement ->
            if (fileTreeElement.isDirectory) {
                true
            } else {
                filesToInclude.any {
                    fileTreeElement.file.absolutePath.endsWith(it)
                }
            }
        }
    } else {
        exclude("*")
    }
}
