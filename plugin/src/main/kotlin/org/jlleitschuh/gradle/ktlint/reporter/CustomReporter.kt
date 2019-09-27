package org.jlleitschuh.gradle.ktlint.reporter

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

class CustomReporter(
    val name: String,
    private val dependencyHandler: DependencyHandler
) {
    /**
     * An id that reporter exposes for ktlint `ServiceLocator`.
     */
    val reporterId: String = name

    /**
     * Generated report file extension.
     */
    var fileExtension: String = reporterId

    /**
     * Reporter [dependency notation](https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.dsl.DependencyHandler.html#N17198).
     *
     * For example it could be:
     * ```
     * "some.group:reporter:0.1.0"
     * project(":custom:reporter")
     * ```
     */
    var dependency: Any? = null
        set(value) {
            field = value
            if (value != null) _dependencyArtifact = dependencyHandler.create(value)
        }

    private var _dependencyArtifact: Dependency? = null
    internal val dependencyArtifact: Dependency get() {
        val artifact = _dependencyArtifact
        requireNotNull(artifact) { "Reporter dependency is not set!" }
        return artifact
    }
}
