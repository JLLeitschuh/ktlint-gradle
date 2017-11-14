package org.jlleitschuh.gradle.ktlint

import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

/**
 * Extension class for configuring the [KtlintPlugin].
 */
open class KtlintExtension {
    /**
     * The version of ktlint to use.
     */
    var version = "0.9.2"
    /**
     * Enable verbose mode.
     */
    var verbose = false
    /**
     * Enable debug mode.
     */
    var debug = false
    /**
     * Enable android mode.
     */
    var android = false
    /**
     * Enable console output mode.
     */
    var outputToConsole = true
    /**
     * Whether or not to allow the build to continue if there are warnings;
     * defaults to {@code false}, as for any other static code analysis tool.
     * <p>
     * Example: `ignoreFailures = true`
     */
    var ignoreFailures = false

    /**
     * Report output formats.
     *
     * Available values: plain, plain_group_by_file, checkstyle, json.
     *
     * Default is empty.
     */
    var reporters: Set<ReporterType> = setOf(ReporterType.PLAIN)
}
