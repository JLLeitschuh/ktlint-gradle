package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Bundling
import org.gradle.util.GradleVersion

internal const val KTLINT_CONFIGURATION_NAME = "ktlint"
internal const val KTLINT_CONFIGURATION_DESCRIPTION = "Main ktlint-gradle configuration"
internal const val KTLINT_RULESET_CONFIGURATION_NAME = "ktlintRuleset"
internal const val KTLINT_RULESET_CONFIGURATION_DESCRIPTION = "All ktlint rulesets dependencies"
internal const val KTLINT_REPORTER_CONFIGURATION_NAME = "ktlintReporter"
internal const val KTLINT_REPORTER_CONFIGURATION_DESCRIPTION = "All ktlint custom reporters dependencies"
internal const val KTLINT_BASELINE_REPORTER_CONFIGURATION_NAME = "ktlintBaselineReporter"
internal const val KTLINT_BASELINE_REPORTER_CONFIGURATION_DESCRIPTION =
    "Provides KtLint baseline reporter required to generate baseline file"

internal fun createKtlintConfiguration(target: Project, extension: KtlintExtension): Configuration =
    target.configurations.maybeCreate(KTLINT_CONFIGURATION_NAME).apply {
        // Configurations in resolved state are not allowed to modify dependencies
        if (state != Configuration.State.UNRESOLVED) return@apply

        description = KTLINT_CONFIGURATION_DESCRIPTION

        isCanBeResolved = true
        isCanBeConsumed = false
        isVisible = false

        // Starting from KtLint 0.41.0 version published artifact has two variants: "external" and "shadowed"
        attributes {
            it.attribute(Bundling.BUNDLING_ATTRIBUTE, target.objects.named(Bundling::class.java, Bundling.EXTERNAL))
        }

        val dependencyProvider = target.provider {
            val ktlintVersion = extension.version.get()
            target.logger.info("Add dependency: ktlint version $ktlintVersion")
            target.dependencies.create("${resolveGroup(ktlintVersion)}:ktlint:$ktlintVersion")
        }
        dependencies.addLater(dependencyProvider)
    }

private fun resolveGroup(ktlintVersion: String) = when {
    SemVer.parse(ktlintVersion) < SemVer(0, 32, 0) -> "com.github.shyiko"
    else -> "com.pinterest"
}

internal fun createKtlintRulesetConfiguration(
    target: Project,
    ktLintConfiguration: Configuration
): Configuration = target
    .configurations.maybeCreate(KTLINT_RULESET_CONFIGURATION_NAME).apply {
        description = KTLINT_RULESET_CONFIGURATION_DESCRIPTION

        isCanBeResolved = true
        isCanBeConsumed = false
        isVisible = false

        ensureConsistencyWith(target, ktLintConfiguration)
    }

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
        isVisible = false

        ensureConsistencyWith(target, ktLintConfiguration)

        withDependencies {
            extension
                .reporterExtension
                .customReporters
                .all {
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
    }

internal fun createKtLintBaselineReporterConfiguration(
    target: Project,
    extension: KtlintExtension,
    ktLintConfiguration: Configuration
): Configuration = target
    .configurations
    .maybeCreate(KTLINT_BASELINE_REPORTER_CONFIGURATION_NAME)
    .apply {
        description = KTLINT_BASELINE_REPORTER_CONFIGURATION_DESCRIPTION

        isCanBeResolved = true
        isCanBeConsumed = false
        isVisible = false

        ensureConsistencyWith(target, ktLintConfiguration)

        withDependencies {
            dependencies.addLater(
                target.provider {
                    val ktlintVersion = extension.version.get()
                    // Baseline reporter is only available starting 0.41.0 release
                    if (SemVer.parse(ktlintVersion) >= SemVer(0, 41, 0)) {
                        target.dependencies.create(
                            "com.pinterest.ktlint:ktlint-reporter-baseline:${extension.version.get()}"
                        )
                    } else {
                        // Adding fake plain reporter as addLater() does not accept `null` value
                        // Generate baseline tasks anyway will not run on KtLint versions < 0.41.0
                        target.dependencies.create(
                            "com.pinterest.ktlint:ktlint-reporter-plain:${extension.version.get()}"
                        )
                    }
                }
            )
        }
    }

private fun Configuration.ensureConsistencyWith(
    target: Project,
    otherConfiguration: Configuration
) {
    if (GradleVersion.version(target.gradle.gradleVersion) >= GradleVersion.version("6.8")) {
        shouldResolveConsistentlyWith(otherConfiguration)
    } else {
        // Inspired by
        // https://android.googlesource.com/platform/tools/base/+/refs/heads/mirror-goog-studio-master-dev/build-system/gradle-core/src/main/java/com/android/build/gradle/internal/dependency/ConstraintHandler.kt
        incoming.beforeResolve {
            val configName = it.name
            otherConfiguration.incoming.resolutionResult.allDependencies { dependency ->
                if (dependency is ResolvedDependencyResult) {
                    val id = dependency.selected.id
                    if (id is ModuleComponentIdentifier) {
                        // using a repository with a flatDir to stock local AARs will result in an
                        // external module dependency with no version.
                        if (!id.version.isNullOrEmpty()) {
                            if (id.module != "listenablefuture" ||
                                id.group != "com.google.guava" ||
                                id.version != "1.0"
                            ) {
                                target.dependencies.constraints.add(
                                    configName,
                                    "${id.group}:${id.module}:${id.version}"
                                ) { constraint ->
                                    constraint.because("${otherConfiguration.name} uses version ${id.version}")
                                    constraint.version { versionConstraint ->
                                        versionConstraint.strictly(id.version)
                                    }
                                }
                            }
                        }
                    } else if (id is ProjectComponentIdentifier &&
                        id.build.isCurrentBuild &&
                        dependency.requested is ModuleComponentSelector
                    ) {
                        // Requested external library has been replaced with the project dependency, so
                        // add the project dependency to the target configuration, so it can be chosen
                        // instead of the external library as well.
                        // We should avoid doing this for composite builds, so we check if the selected
                        // project is from the current build.
                        target.dependencies.add(
                            configName,
                            target.dependencies.project(mapOf("path" to id.projectPath))
                        )
                    }
                }
            }
        }
    }
}
