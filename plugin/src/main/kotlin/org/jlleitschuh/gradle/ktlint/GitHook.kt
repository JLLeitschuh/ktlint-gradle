package org.jlleitschuh.gradle.ktlint

import javax.inject.Inject
import org.eclipse.jgit.lib.RepositoryBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.intellij.lang.annotations.Language

internal const val FILTER_INCLUDE_PROPERTY_NAME = "internalKtlintGitFilter"

@Language("Bash")
internal val shShebang = """
#!/bin/sh
set -e

""".trimIndent()

internal const val startHookSection = "######## KTLINT-GRADLE HOOK START ########\n"
internal const val endHookSection = "######## KTLINT-GRADLE HOOK END ########\n"

private fun generateGradleCommand(
    taskName: String,
    gradleRootDirPrefix: String
): String {
    val gradleCommand = if (gradleRootDirPrefix.isNotEmpty()) {
        "./$gradleRootDirPrefix/gradlew -p ./$gradleRootDirPrefix"
    } else {
        "./gradlew"
    }
    return "$gradleCommand --quiet $taskName -P$FILTER_INCLUDE_PROPERTY_NAME=\"${'$'}CHANGED_FILES\""
}

private fun generateGitCommand(
    gradleRootDirPrefix: String
): String = if (gradleRootDirPrefix.isEmpty()) {
    "git --no-pager diff --name-status --no-color --cached"
} else {
    "git --no-pager diff --name-status --no-color --cached -- $gradleRootDirPrefix/"
}

@Language("Sh")
internal fun generateGitHook(
    taskName: String,
    gradleRootDirPrefix: String
) = """

CHANGED_FILES="${'$'}(${generateGitCommand(gradleRootDirPrefix)} | awk '$1 != "D" && $2 ~ /\.kts|\.kt/ { print $2}')"

if [ -z "${'$'}CHANGED_FILES" ]; then
    echo "No Kotlin staged files."
    exit 0
fi;

echo "Running ktlint over these files:"
echo "${'$'}CHANGED_FILES"

${generateGradleCommand(taskName, gradleRootDirPrefix)}

echo "Completed ktlint run."

echo "${'$'}CHANGED_FILES" | while read -r file; do
    if [ -f ${'$'}file ]; then
        git add ${'$'}file
    fi
done

echo "Completed ktlint hook."

""".trimIndent()

internal fun KtlintPlugin.PluginHolder.addGitHookTasks() {
    if (target.rootProject == target) {
        addInstallGitHookFormatTask()
        addInstallGitHookCheckTask()
    }
}

private fun KtlintPlugin.PluginHolder.addInstallGitHookFormatTask() {
    target.tasks.register(
        INSTALL_GIT_HOOK_FORMAT_TASK,
        KtlintInstallGitHookTask::class.java
    ) {
        it.description = "Adds git hook to run ktlintFormat on changed files"
        it.group = HELP_GROUP
        it.taskName.set(FORMAT_PARENT_TASK_NAME)
        it.hookName.set("pre-commit")
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
        it.hookName.set("pre-commit")
    }
}

open class KtlintInstallGitHookTask @Inject constructor(
    objectFactory: ObjectFactory
) : DefaultTask() {
    @get:Input
    internal val taskName: Property<String> = objectFactory.property(String::class.java)

    @get:Input
    internal val hookName: Property<String> = objectFactory.property(String::class.java)

    @TaskAction
    fun installHook() {
        val repo = RepositoryBuilder().findGitDir(project.projectDir).setMustExist(false).build()
        if (!repo.objectDatabase.exists()) {
            logger.warn("No git folder was found!")
            return
        }

        logger.info(".git directory path: ${repo.directory}")
        val gitHookFile = repo.directory.resolve("hooks/${hookName.get()}")
        logger.info("Hook file: $gitHookFile")
        if (!gitHookFile.exists()) {
            gitHookFile.createNewFile()
            gitHookFile.setExecutable(true)
        }
        val gradleRootDirPrefix = project.rootDir.relativeTo(repo.workTree).path

        if (gitHookFile.length() == 0L) {
            gitHookFile.writeText(
                "$shShebang$startHookSection${generateGitHook(taskName.get(), gradleRootDirPrefix)}$endHookSection"
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
                "$startHookSection${generateGitHook(taskName.get(), gradleRootDirPrefix)}"
            )
            gitHookFile.writeText(hookContent)
        } else {
            gitHookFile.appendText(
                "$startHookSection${generateGitHook(taskName.get(), gradleRootDirPrefix)}$endHookSection"
            )
        }
    }
}

internal fun BaseKtlintCheckTask.applyGitFilter() {
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
