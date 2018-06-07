package org.jlleitschuh.gradle.ktlint

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class KtlintApplyToIdeaTask @Inject constructor(
    objectFactory: ObjectFactory
) : DefaultTask() {

    @get:Classpath
    val classpath: ConfigurableFileCollection = project.files()

    @get:Input
    val android: Property<Boolean> = objectFactory.booleanProperty()

    @TaskAction
    fun generate() {
        project.javaexec {
            it.classpath = classpath
            it.main = "com.github.shyiko.ktlint.Main"
            // -y here to auto-overwrite existing IDEA code style
            it.args("--apply-to-idea-project", "-y")
            if (android.get()) {
                it.args("--android")
            }
        }
    }

    private fun ObjectFactory.booleanProperty() =
            property(Boolean::class.javaObjectType)
}
