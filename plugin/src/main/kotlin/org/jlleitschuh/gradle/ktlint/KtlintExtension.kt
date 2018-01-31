package org.jlleitschuh.gradle.ktlint

@Deprecated("Moved to org.jlleitschuh.gradle.ktlint.reporter package")
typealias ReporterType = org.jlleitschuh.gradle.ktlint.reporter.ReporterType

/**
 * Extension class for configuring the [KtlintPlugin].
 */
open class KtlintExtension {
    /**
     * The version of ktlint to use.
     */
    var version = "0.15.0"
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
     * **Note** for Gradle scripts: for now all values should be uppercase due to bug in Gradle.
     *
     * Default is empty.
     */
    var reporters: Array<ReporterType> = emptyArray()

    /**
     * Report output format.
     *
     * Available values: plain, plain_group_by_file, checkstyle, json.
     *
     * Default is plain.
     */
    @Deprecated(message = "Ktlint introduced multi output support since 0.11.1 version",
            replaceWith = ReplaceWith("reporters = arrayOf()", "reporter"))
    var reporter: ReporterType? = ReporterType.PLAIN
}
