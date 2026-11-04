package org.jlleitschuh.gradle.ktlint

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.newInstance
import org.jlleitschuh.gradle.ktlint.reporter.CustomReporter
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import javax.inject.Inject

/**
 * Extension class for configuring the [KtlintPlugin].
 * @param filterTargetApplier When [KtlintExtension.filter] is called, this function is executed.
 */
@Suppress("UnstableApiUsage")
open class KtlintExtension @Inject internal constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout,
    private val filterTargetApplier: FilterApplier,
    kotlinScriptAdditionalPathApplier: KotlinScriptAdditionalPathApplier
) {
    val reporterExtension = objectFactory.newInstance(ReporterExtension::class)

    /**
     * The version of KtLint to use.
     */
    val version: Property<String> = objectFactory.property { set("1.5.0") }

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
     * Only include rules that were introduced in KtLint versions up to and including this version.
     * When set, rules with @SinceKtlint annotations indicating they were added after this version will be excluded.
     * Rules without the annotation (from older ktlint versions) are always included for backward compatibility.
     *
     * Format: "0.46.0", "0.47.1", etc.
     * Default: null (no filtering - all rules are included)
     *
     * Example: `maxRuleVersion.set("0.46.0")` will exclude rules added in 0.47.0 and later.
     *
     * @since ktlint `1.8.0`
     */
    val maxRuleVersion: Property<String> = objectFactory.property(String::class.java)

    /**
     * Provide additional `.editorconfig` properties, that are not in the project or project parent folders.
     */
    val additionalEditorconfig: MapProperty<String, String> =
        objectFactory.mapProperty(String::class.java, String::class.java)
            .apply {
                convention(emptyMap())
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

    open class ReporterExtension @Inject constructor(
        objectFactory: ObjectFactory
    ) {
        internal val reporters: SetProperty<ReporterType> = objectFactory.setProperty {
            set(emptySet())
        }
        val customReporters: NamedDomainObjectContainer<CustomReporter> =
            objectFactory.domainObjectContainer(CustomReporter::class) { CustomReporter(it) }

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
        fun customReporters(configuration: Action<NamedDomainObjectContainer<CustomReporter>>) {
            configuration.execute(customReporters)
        }
    }
}
