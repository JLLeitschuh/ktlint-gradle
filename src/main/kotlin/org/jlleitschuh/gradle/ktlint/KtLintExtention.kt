package org.jlleitschuh.gradle.ktlint;

/**
 * Extension class for configuring the [KtLintPlugin].
 */
open class KtLintExtention {
    /**
     * The version of ktlint to use.
     */
    var version = "0.6.1"
    /**
     * Enable verbose mode.
     */
    var verbose = false
    /**
     * Enable debug mode.
     */
    var debug = false
}
