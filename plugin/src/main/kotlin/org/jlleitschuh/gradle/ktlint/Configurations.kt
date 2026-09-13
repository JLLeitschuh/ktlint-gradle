package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Bundling

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

        // Starting from KtLint 0.41.0 version published artifact has two variants: "external" and "shadowed"
        c.attributes {
            it.attribute(Bundling.BUNDLING_ATTRIBUTE, target.objects.named(Bundling::class.java, Bundling.EXTERNAL))
        }
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
): Configuration = target
    .configurations.maybeCreate(KTLINT_RULESET_CONFIGURATION_NAME).apply {
        description = KTLINT_RULESET_CONFIGURATION_DESCRIPTION

        isCanBeResolved = true
        isCanBeConsumed = false

        shouldResolveConsistentlyWith(ktLintConfiguration)
        dependencies.addLater(
            extension.version.map {
                val semver = SemVer.parse(it)
                target.dependencies.create(determineKtlintGav(semver))
            }
        )
    }

@Suppress("UnstableApiUsage")
internal fun createKtLintReporterConfiguration(
    target: Project,
    extension: KtlintExtension,
    ktLintConfiguration: Configuration
): Configuration = target
    .configurations
    .maybeCreate(KTLINT_REPORTER_CONFIGURATION_NAME)
    .apply {
        description = KTLINT_REPORTER_CONFIGURATION_DESCRIPTION

        isCanBeResolved = true
        isCanBeConsumed = false

        shouldResolveConsistentlyWith(ktLintConfiguration)

        withDependencies {
            extension
                .reporterExtension
                .customReporters
                .forEach {
                    dependencies.addLater(
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

        // Workaround for gradle 6 https://github.com/gradle/gradle/issues/13255
        val oldProp = target.objects.listProperty(Dependency::class.java)
        dependencies.addAllLater(
            oldProp.value(
                extension.version.map { version ->
                    if (SemVer.parse(version) >= SemVer(1, 0, 0)) {
                        // this transitive dep was introduced in ktlint 1.0, but for some reason, it is not picked up automatically
                        listOf(target.dependencies.create("io.github.oshai:kotlin-logging:5.1.0"))
                    } else {
                        listOf()
                    }
                }
            )
        )
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
