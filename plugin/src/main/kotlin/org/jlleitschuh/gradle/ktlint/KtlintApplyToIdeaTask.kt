package org.jlleitschuh.gradle.ktlint

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

@Suppress("UnstableApiUsage")
open class KtlintApplyToIdeaTask @Inject constructor(
    objectFactory: ObjectFactory
) : DefaultTask() {

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
            it.main = "com.pinterest.ktlint.Main"

            // Global flags
            if (android.get()) {
                it.args(
                    "--android"
                )
            }

            // Subcommand
            if (globally.get()) {
                it.args("applyToIDEA")
            } else {
                it.args("applyToIDEAProject")
            }

            // Subcommand parameters
            // -y here to auto-overwrite existing IDEA code style
            it.args("-y")
        }
    }
}
