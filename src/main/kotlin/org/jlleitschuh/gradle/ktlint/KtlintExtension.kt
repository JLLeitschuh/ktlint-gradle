package org.jlleitschuh.gradle.ktlint

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
     * Whether or not to allow the build to continue if there are warnings;
     * defaults to {@code false}, as for any other static code analysis tool.
     * <p>
     * Example: {@code ignoreFailures = true}
     */
    var ignoreFailures = false

    /**
     * Report output format.
     *
     * Available values: plain, plain_group_by_file, checkstyle, json.
     *
     * Default is plain.
     */
    var reporter: ReporterType = ReporterType.PLAIN
}
