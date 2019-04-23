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
            it.main = resolveMainClassName(ktlintVersion.get())
            if (globally.get()) {
                it.args("--apply-to-idea")
            } else {
                it.args("--apply-to-idea-project")
            }
            // -y here to auto-overwrite existing IDEA code style
            it.args("-y")
            if (android.get()) {
                it.args("--android")
            }
        }
    }
}
