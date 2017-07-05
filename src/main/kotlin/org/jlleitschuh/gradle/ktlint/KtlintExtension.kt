package org.jlleitschuh.gradle.ktlint;

/**
 * Extension class for configuring the [KtlintPlugin].
 */
open class KtlintExtension {
    /**
     * The version of ktlint to use.
     */
    var version = "0.8.1"
    /**
     * Enable verbose mode.
     */
    var verbose = false
    /**
     * Enable debug mode.
     */
    var debug = false
}
