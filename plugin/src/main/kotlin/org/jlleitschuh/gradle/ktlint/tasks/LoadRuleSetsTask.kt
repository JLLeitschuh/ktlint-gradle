package org.jlleitschuh.gradle.ktlint.tasks

import net.swiftzer.semver.SemVer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import org.jlleitschuh.gradle.ktlint.intermediateResultsBuildDir
import org.jlleitschuh.gradle.ktlint.worker.RuleSetLoaderWorkAction
import javax.inject.Inject

/**
 * Task to load KtLint `RuleSet` [Set] and serialize it into [loadedRuleSets] file.
 */
@Suppress("UnstableApiUsage")
internal abstract class LoadRuleSetsTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout
) : DefaultTask() {
    @get:Classpath
    internal abstract val ktLintClasspath: ConfigurableFileCollection

    @get:Classpath
    internal abstract val ruleSetsClasspath: ConfigurableFileCollection

    @get:Input
    internal abstract val ktLintVersion: Property<String>

    @get:Input
    internal abstract val enableExperimentalRules: Property<Boolean>

    @get:Input
    internal abstract val disabledRules: SetProperty<String>

    @get:OutputFile
    internal val loadedRuleSets: RegularFileProperty = objectFactory.fileProperty().convention(
        projectLayout.intermediateResultsBuildDir("ruleSets.bin")
    )

    @TaskAction
    fun loadRuleSets() {
        checkExperimentalRulesSupportedKtLintVersion()
        checkDisabledRulesSupportedKtLintVersion()

        val queue = workerExecutor.classLoaderIsolation { workerExecutor ->
            workerExecutor.classpath.from(ktLintClasspath, ruleSetsClasspath)
        }

        queue.submit(RuleSetLoaderWorkAction::class.java) { params ->
            params.disabledRules.set(disabledRules)
            params.enableExperimentalRules.set(enableExperimentalRules)
            params.serializeResultIntoFile.set(loadedRuleSets)
        }
    }

    private fun checkExperimentalRulesSupportedKtLintVersion() {
        if (enableExperimentalRules.get() &&
            SemVer.parse(ktLintVersion.get()) < SemVer(0, 31, 0)
        ) {
            throw GradleException("Experimental rules are supported since 0.31.0 ktlint version.")
        }
    }

    private fun checkDisabledRulesSupportedKtLintVersion() {
        if (disabledRules.get().isNotEmpty() &&
            SemVer.parse(ktLintVersion.get()) < SemVer(0, 34, 2)
        ) {
            throw GradleException("Rules disabling is supported since 0.34.2 ktlint version.")
        }
    }

    companion object {
        internal const val LOAD_RULE_SETS_TASK = "loadKtlintRuleSets"
        internal const val DESCRIPTION = "Preloads required Ktlint RuleSets."
    }
}
