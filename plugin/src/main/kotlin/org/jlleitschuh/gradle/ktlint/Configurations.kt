package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.provider.Provider
import org.gradle.internal.extensions.core.serviceOf
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

internal const val KTLINT_CONFIGURATION_NAME = "ktlint"
internal const val KTLINT_CONFIGURATION_DESCRIPTION = "Main ktlint-gradle configuration"
internal const val KTLINT_RULESET_CONFIGURATION_NAME = "ktlintRuleset"
internal const val KTLINT_RULESET_CONFIGURATION_DESCRIPTION = "All ktlint rulesets dependencies"
internal const val KTLINT_REPORTER_CONFIGURATION_NAME = "ktlintReporter"
internal const val KTLINT_REPORTER_CONFIGURATION_DESCRIPTION = "All ktlint custom reporters dependencies"
internal const val KTLINT_BASELINE_REPORTER_CONFIGURATION_NAME = "ktlintBaselineReporter"
internal const val KTLINT_BASELINE_REPORTER_CONFIGURATION_DESCRIPTION =
    "Provides KtLint baseline reporter required to generate baseline file"

internal fun determineKtlintArtifact(ktlintVersion: SemVer): String {
    val artifact = if (ktlintVersion.major > 0) "ktlint-cli" else "ktlint"
    return artifact
}

internal fun determineKtlintGroup(ktlintVersion: SemVer): String {
    val groupId = when (ktlintVersion.major) {
        0 -> "com.pinterest.ktlint"
        1 -> "com.pinterest.ktlint"
        else -> "io.github.ktlint.core"
    }
    return groupId
}

internal fun determineKtlintGav(ktlintVersion: SemVer): String {
    return "${determineKtlintGroup(ktlintVersion)}:${determineKtlintArtifact(ktlintVersion)}:$ktlintVersion"
}

internal fun createKtlintConfiguration(target: Project, extension: KtlintExtension): Configuration =
    target.configurations.create(KTLINT_CONFIGURATION_NAME) { c ->
        c.description = KTLINT_CONFIGURATION_DESCRIPTION

        c.isCanBeResolved = true
        c.isCanBeConsumed = false
        target.serviceOf<JvmPluginServices>().configureAsRuntimeElements(c)

        c.dependencies.addAllLater(
            extension.version.map {
                val semver = SemVer.parse(it)
                target.logger.info("Add dependency: ktlint version $it")
                listOf(target.dependencies.create(determineKtlintGav(semver)))
            }
        )
    }

@Suppress("UnstableApiUsage")
internal fun createKtlintRulesetConfiguration(
    target: Project,
    ktLintConfiguration: Configuration,
    extension: KtlintExtension
): Configuration = target.configurations.create(KTLINT_RULESET_CONFIGURATION_NAME) { c ->
    c.description = KTLINT_RULESET_CONFIGURATION_DESCRIPTION

    c.isCanBeResolved = true
    c.isCanBeConsumed = false
    target.serviceOf<JvmPluginServices>().configureAsRuntimeElements(c)
    c.shouldResolveConsistentlyWith(ktLintConfiguration)
    c.dependencies.addLater(
        extension.version.map {
            val semver = SemVer.parse(it)
            target.dependencies.create(determineKtlintGav(semver))
        }
    )
}

fun determineBuiltInReporterDependencies(
    ktlintVersion: Provider<SemVer>,
    reporterTypes: Set<ReporterType>
): Provider<List<String>> {
    return ktlintVersion.map { version ->
        if (version.major < 0) {
            emptyList()
        } else {
            val group = determineKtlintGroup(version)
            reporterTypes.map { reporterType ->
                when (reporterType) {
                    ReporterType.CHECKSTYLE -> "$group:ktlint-cli-reporter-checkstyle:$version"
                    ReporterType.SARIF -> "$group:ktlint-cli-reporter-sarif:$version"
                    ReporterType.JSON -> "$group:ktlint-cli-reporter-json:$version"
                    ReporterType.PLAIN -> "$group:ktlint-cli-reporter-plain:$version"
                    ReporterType.HTML -> "$group:ktlint-cli-reporter-html:$version"
                    ReporterType.PLAIN_GROUP_BY_FILE -> "$group:ktlint-cli-reporter-plain:$version"
                }
            }
        }
    }
}

@Suppress("UnstableApiUsage")
internal fun createKtLintReporterConfiguration(
    target: Project,
    extension: KtlintExtension,
    ktLintConfiguration: Configuration
): NamedDomainObjectProvider<Configuration> = target
    .configurations.register(KTLINT_REPORTER_CONFIGURATION_NAME) { c ->
        c.description = KTLINT_REPORTER_CONFIGURATION_DESCRIPTION

        c.isCanBeResolved = true
        c.isCanBeConsumed = false
        target.serviceOf<JvmPluginServices>().configureAsRuntimeElements(c)
        c.shouldResolveConsistentlyWith(ktLintConfiguration)

        val ktlintVersionProvider = extension.version.map { SemVer.parse(it) }
        c.dependencies.addAllLater(
            extension.reporterExtension.reporters.flatMap { reporters ->
                determineBuiltInReporterDependencies(ktlintVersionProvider, reporters).map {
                    it.map { target.dependencies.create(it) }
                }
            }
        )
        c.withDependencies {
            extension
                .reporterExtension
                .customReporters
                .forEach {
                    c.dependencies.addLater(
                        target.provider {
                            val reporterDependency = it.dependency
                            requireNotNull(reporterDependency) {
                                "Reporter ${it.reporterId} dependency is not set!"
                            }
                            target.dependencies.create(reporterDependency)
                        }
                    )
                }
        }
    }

@Suppress("UnstableApiUsage")
internal fun createKtLintBaselineReporterConfiguration(
    target: Project,
    ktLintConfiguration: Configuration
): Configuration = target.configurations.create(KTLINT_BASELINE_REPORTER_CONFIGURATION_NAME) { c ->
    c.description = KTLINT_BASELINE_REPORTER_CONFIGURATION_DESCRIPTION

    c.isCanBeResolved = true
    c.isCanBeConsumed = false

    c.shouldResolveConsistentlyWith(ktLintConfiguration)
}
