package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

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
        if (!globally.get() && !isApplyToIdeaPerProjectAvailable()) {
            logger.error("Apply per project in only available from ktlint 0.22.0")
            throw StopExecutionException("Apply per project in only available from ktlint 0.22.0")
        }

        project.javaexec {
            it.classpath = classpath
            it.main = "com.github.shyiko.ktlint.Main"
            if (globally.get()) {
                it.args("--apply-to-idea")
            } else {
                it.args("--apply-to-idea-project")
            }
            // -y here to auto-overwrite existing IDEA code style
            it.args("-y")
            if (android.get() && ktlintVersion.isAndroidFlagAvailable()) {
                it.args("--android")
            }
        }
    }

    /**
     * Checks if apply code style to IDEA IDE per project is available.
     *
     * Available since KtLint version `0.22.0`.
     */
    private fun isApplyToIdeaPerProjectAvailable() =
        SemVer.parse(ktlintVersion.get()) >= SemVer(0, 22, 0)
}
