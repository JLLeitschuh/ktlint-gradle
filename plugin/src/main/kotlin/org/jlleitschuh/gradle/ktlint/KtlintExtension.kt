package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.util.PatternFilterable
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

/**
 * Extension class for configuring the [KtlintPlugin].
 * @param filterTargetApplier When [KtlintExtension.filter] is called, this function is executed.
 */
open class KtlintExtension
internal constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    private val filterTargetApplier: FilterApplier,
    private val kotlinScriptAdditionalPathApplier: KotlinScriptAdditionalPathApplier
) {
    /**
     * The version of ktlint to use.
     */
    val version: Property<String> = objectFactory.property { set("0.34.2") }

    /**
     * Enable verbose mode.
     */
    val verbose: Property<Boolean> = objectFactory.property { set(false) }
    /**
     * Enable debug mode.
     */
    val debug: Property<Boolean> = objectFactory.property { set(false) }
    /**
     * Enable android mode.
     */
    val android: Property<Boolean> = objectFactory.property { set(false) }
    /**
     * Enable console output mode.
     */
    val outputToConsole: Property<Boolean> = objectFactory.property { set(true) }
    /**
     * Enabled colored output to console.
     */
    val coloredOutput: Property<Boolean> = objectFactory.property { set(true) }
    /**
     * Whether or not to allow the build to continue if there are warnings;
     * defaults to {@code false}, as for any other static code analysis tool.
     * <p>
     * Example: `ignoreFailures = true`
     */
    val ignoreFailures: Property<Boolean> = objectFactory.property { set(false) }

    /**
     * Report output formats.
     *
     * Available values: plain, plain_group_by_file, checkstyle, json.
     *
     * **Note** for Gradle scripts: for now all values should be uppercase due to bug in Gradle.
     *
     * Default is plain.
     */
    val reporters: SetProperty<ReporterType> = objectFactory.setProperty {
        set(setOf(ReporterType.PLAIN))
    }

    /**
     * Enable experimental ktlint rules.
     *
     * You can find [here](https://github.com/pinterest/ktlint/blob/master/ktlint-ruleset-experimental/src/main/kotlin/com/pinterest/ktlint/ruleset/experimental/ExperimentalRuleSetProvider.kt)
     * list of experimental rules that will be enabled.
     *
     * @since ktlint `0.31.0`
     */
    val enableExperimentalRules: Property<Boolean> = objectFactory.property {
        set(false)
    }

    /**
     * Provide additional `.editorconfig` file, that are not in the project or project parent folders.
     */
    val additionalEditorconfigFile: RegularFileProperty = newFileProperty(objectFactory, projectLayout)

    /**
     * Disable particular rules, by default enabled in ktlint, using rule id.
     *
     * @since ktlint `0.34.2`
     */
    val disabledRules: SetProperty<String> = objectFactory.setProperty {
        set(emptySet())
    }

    private val kscriptExtension = KScriptExtension(kotlinScriptAdditionalPathApplier)

    /**
     * Provide additional, relative to the project base dir, paths that contains kotlin script files.
     */
    fun kotlinScriptAdditionalPaths(action: Action<KScriptExtension>) {
        action.execute(kscriptExtension)
    }

    /**
     * Filter sources by applying exclude or include specs/patterns.
     *
     * See [PatternFilterable](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/util/PatternFilterable.html)
     * for details how apply exclude or include specs/patterns.
     */
    fun filter(filterAction: Action<PatternFilterable>) {
        filterTargetApplier(filterAction)
    }

    class KScriptExtension(
        private val ksApplier: KotlinScriptAdditionalPathApplier
    ) {
        /**
         * Adds given [fileTree] to [KOTLIN_SCRIPT_CHECK_TASK]/[KOTLIN_SCRIPT_FORMAT_TASK] tasks search.
         */
        fun include(fileTree: ConfigurableFileTree) {
            ksApplier(fileTree)
        }
    }
}
