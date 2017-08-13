package org.jlleitschuh.gradle.ktlint

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

    /**
     * Report output format.
     *
     * Available values: plain, plain_group_by_file, checkstyle, json.
     *
     * Default is plain.
     */
    var reporter: ReporterType  = ReporterType.PLAIN

    /**
     * Supported reporter types.
     */
    enum class ReporterType(val reporterName: String, val availableSinceVersion: String, val fileExtension: String) {
        PLAIN("plain", "0.9.0", "txt"),
        PLAIN_GROUP_BY_FILE("plain?group_by_file", "0.9.0", "txt"),
        CHECKSTYLE("checkstyle", "0.9.0", "xml"),
        JSON("json", "0.9.0", "json");
    }
}
