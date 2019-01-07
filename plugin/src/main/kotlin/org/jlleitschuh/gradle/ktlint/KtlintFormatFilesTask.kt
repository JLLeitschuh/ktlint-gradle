package org.jlleitschuh.gradle.ktlint

import org.gradle.api.file.FileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.options.Option
import javax.inject.Inject

open class KtlintFormatFilesTask @Inject constructor(
    objectFactory: ObjectFactory
) : KtlintFormatTask(objectFactory) {

    @get:Input
    var filesToFormat: List<String> = emptyList()

    @Option(
        option = "files",
        description = "(relative) paths of files to be formatted; line separated list"
    )
    fun setFiles(files: String) {
        filesToFormat = files.split("\n")
    }

    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    override fun getSource(): FileTree {
        return super.getSource()
            .filter { file -> filesToFormat.any { file.absolutePath.endsWith(it) } }
            .asFileTree
    }
}
