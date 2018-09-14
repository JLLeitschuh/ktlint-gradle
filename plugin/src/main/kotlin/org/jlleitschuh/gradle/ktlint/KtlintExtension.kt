package org.jlleitschuh.gradle.ktlint

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

/**
 * Extension class for configuring the [KtlintPlugin].
 */
open class KtlintExtension(
    objectFactory: ObjectFactory
) {
    /**
     * The version of ktlint to use.
     */
    val version: Property<String> = objectFactory.property { set("0.27.0") }

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
     * Whether or not to allow the build to continue if there are warnings;
     * defaults to {@code false}, as for any other static code analysis tool.
     * <p>
     * Example: `ignoreFailures = true`
     */
    val ignoreFailures: Property<Boolean> = objectFactory.property { set(false) }
    /**
     * The ruleset(s) of ktlint to use.
     */
    var ruleSets: Array<String> = arrayOf()

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
}
