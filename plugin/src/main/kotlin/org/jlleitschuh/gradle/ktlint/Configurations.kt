package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

internal const val KTLINT_CONFIGURATION_NAME = "ktlint"
internal const val KTLINT_CONFIGURATION_DESCRIPTION = "Main ktlint-gradle configuration"

internal fun createKtlintConfiguration(target: Project, extension: KtlintExtension) =
    target.configurations.maybeCreate(KTLINT_CONFIGURATION_NAME).apply {
        description = KTLINT_CONFIGURATION_DESCRIPTION
        val dependencyProvider = target.provider<Dependency> {
            val ktlintVersion = extension.version.get()
            target.logger.info("Add dependency: ktlint version $ktlintVersion")
            target.dependencies.create(
                mapOf(
                    "group" to resolveGroup(ktlintVersion),
                    "name" to "ktlint",
                    "version" to ktlintVersion
                )
            )
        }
        dependencies.addLater(dependencyProvider)
    }

private fun resolveGroup(ktlintVersion: String) = when {
    SemVer.parse(ktlintVersion) < SemVer(0, 32, 0) -> "com.github.shyiko"
    else -> "com.pinterest"
}
