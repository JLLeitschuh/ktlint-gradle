package org.jlleitschuh.gradle.ktlint

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.util.ConfigureUtil
import org.jlleitschuh.gradle.ktlint.reporter.CustomReporter
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

/**
 * Extension class for configuring the [KtlintPlugin].
 * @param filterTargetApplier When [KtlintExtension.filter] is called, this function is executed.
 */
@Suppress("UnstableApiUsage")
open class KtlintExtension
internal constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    customReportersContainer: NamedDomainObjectContainer<CustomReporter>,
    private val filterTargetApplier: FilterApplier,
    kotlinScriptAdditionalPathApplier: KotlinScriptAdditionalPathApplier
) {
    internal val reporterExtension = ReporterExtension(
        customReportersContainer,
        objectFactory
    )

    /**
     * The version of KtLint to use.
     */
    val version: Property<String> = objectFactory.property { set("0.46.1") }

    /**
     * Enable relative paths in reports
     */
    val relative: Property<Boolean> = objectFactory.property { set(false) }

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
     * Specify the color of the terminal output.
     */
    val outputColorName: Property<String> = objectFactory.property { set("") }
    /**
     * Whether or not to allow the build to continue if there are warnings;
     * defaults to {@code false}, as for any other static code analysis tool.
     * <p>
     * Example: `ignoreFailures = true`
     */
    val ignoreFailures: Property<Boolean> = objectFactory.property { set(false) }

    /**
     * Configure Ktlint output reporters.
     *
     * _By default_ `plain` reporter is enabled, if no other reporter is configured, otherwise you need to specify
     * it explicitly.
     */
    fun reporters(action: Action<ReporterExtension>) {
        action.execute(reporterExtension)
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
    val additionalEditorconfigFile: RegularFileProperty = objectFactory.fileProperty()

    /**
     * Disable particular rules, by default enabled in ktlint, using rule id.
     *
     * @since ktlint `0.34.2`
     */
    val disabledRules: SetProperty<String> = objectFactory.setProperty {
        set(emptySet())
    }

    /**
     * Baseline file location.
     *
     * Default location is `<projectDir>/config/ktlint/baseline.xml`.
     *
     * @since KtLint `0.41.0`
     */
    val baseline: RegularFileProperty = objectFactory.fileProperty()
        .convention(
            projectLayout.projectDirectory.dir("config").dir("ktlint").file("baseline.xml")
        )

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
         * Adds given [fileTree] to kotlin script check/format tasks search.
         */
        fun include(fileTree: ConfigurableFileTree) {
            ksApplier(fileTree)
        }
    }

    class ReporterExtension(
        val customReporters: NamedDomainObjectContainer<CustomReporter>,
        objectFactory: ObjectFactory
    ) {
        internal val reporters: SetProperty<ReporterType> = objectFactory.setProperty {
            set(emptySet())
        }

        /**
         * Use one of default Ktlint output reporter
         *
         * _By default_ `plain` type is enabled if no reporter is explicitly specified.
         *
         * @param reporterType one of `plain`, `plain_group_by_file`, `checkstyle`, `json`, `sarif`.
         */
        fun reporter(reporterType: ReporterType) {
            reporters.add(reporterType)
        }

        /**
         * Add 3rd party reporters.
         */
        fun customReporters(configuration: Closure<NamedDomainObjectContainer<CustomReporter>>) {
            // This method is needed for Groovy interop
            // See https://discuss.gradle.org/t/multi-level-dsl-for-plugin-extension/19029/16
            ConfigureUtil.configure(configuration, customReporters)
        }
    }
}
