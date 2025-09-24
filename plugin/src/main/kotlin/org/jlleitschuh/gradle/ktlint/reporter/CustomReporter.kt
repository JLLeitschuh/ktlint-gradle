package org.jlleitschuh.gradle.ktlint.reporter

import java.io.Serializable

/**
 * @param name required for Groovy interop, same as [reporterId]
 * @param reporterId an id that reporter exposes for ktlint `ServiceLocator`
 * @param fileExtension generated report file extension
 * @param dependency reporter [dependency notation](https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.dsl.DependencyHandler.html#N17198).
 *
 * For example, [dependency] could have following notation:
 * ```
 * "some.group:reporter:0.1.0"
 * project(":custom:reporter")
 * ```
 */
data class CustomReporter(
    val name: String,
    val reporterId: String = name,
    var fileExtension: String = reporterId,
    @Transient var dependency: Any? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 2012775L
    }
}
