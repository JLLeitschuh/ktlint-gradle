package org.jlleitschuh.gradle.ktlint

import javax.inject.Inject
import net.swiftzer.semver.SemVer
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@Suppress("UnstableApiUsage")
open class KtlintApplyToIdeaTask @Inject constructor(
    objectFactory: ObjectFactory
) : DefaultTask() {
    
    init {
        group = "ktlint"
    }

    @get:Classpath
    val classpath: ConfigurableFileCollection = project.files()

    @get:Input
    val android: Property<Boolean> = objectFactory.property()

    @get:Input
    val globally: Property<Boolean> = objectFactory.property()

    @get:Input
    val ktlintVersion: Property<String> = objectFactory.property()

    @TaskAction
    fun generate() {
        project.javaexec {
            it.classpath = classpath
            it.main = resolveMainClassName(ktlintVersion.get())

            // Global flags
            if (android.get()) {
                it.args("--android")
            }

            // Subcommand
            if (globally.get()) {
                it.args(getApplyToIdeaCommand())
            } else {
                it.args(getApplyToProjectCommand())
            }

            // Subcommand parameters
            // -y here to auto-overwrite existing IDEA code style
            it.args("-y")
        }
    }

    private fun getApplyToIdeaCommand() =
        if (SemVer.parse(ktlintVersion.get()) >= SemVer(0, 35, 0)) {
            "applyToIDEA"
        } else {
            "--apply-to-idea"
        }

    private fun getApplyToProjectCommand() =
        if (SemVer.parse(ktlintVersion.get()) >= SemVer(0, 35, 0)) {
            "applyToIDEAProject"
        } else {
            "--apply-to-idea-project"
        }
}
