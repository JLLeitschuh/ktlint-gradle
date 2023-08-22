package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
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

        // Workaround for gradle 6 https://github.com/gradle/gradle/issues/13255
        val oldProp = target.objects.listProperty(Dependency::class.java)
        dependencies.addAllLater(
            oldProp.value(
                extension.version.map {
                    if (SemVer.parse(it) < SemVer(1, 0, 0)) {
                        target.logger.info("Add dependency: ktlint version $it")
                        listOf(target.dependencies.create("com.pinterest:ktlint:$it"))
                    } else {
                        target.logger.info("Add dependencies: ktlint version $it")
                        listOf(
                            target.dependencies.create("com.pinterest.ktlint:ktlint-cli:$it"),
                            // this transitive dep was introduced in ktlint 1.0, but for some reason, it is not picked up automatically
                            target.dependencies.create("io.github.oshai:kotlin-logging:5.1.0")
                        )
                    }
                }
            )
        )
    }

internal fun createKtlintRulesetConfiguration(
    target: Project,
    ktLintConfiguration: Configuration,
    extension: KtlintExtension
): Configuration = target
    .configurations.maybeCreate(KTLINT_RULESET_CONFIGURATION_NAME).apply {
        description = KTLINT_RULESET_CONFIGURATION_DESCRIPTION

        isCanBeResolved = true
        isCanBeConsumed = false
        isVisible = false

        ensureConsistencyWith(target, ktLintConfiguration)
        dependencies.addLater(
            target.provider {
                val ktlintVersion = extension.version.get()
                target.dependencies.create("com.pinterest.ktlint:ktlint-ruleset-standard:$ktlintVersion")
            }
        )
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

        //     withDependencies {
        // Workaround for gradle 6 https://github.com/gradle/gradle/issues/13255
        val oldProp = target.objects.listProperty(Dependency::class.java)
        dependencies.addAllLater(
            oldProp.value(
                extension.version.map { version ->
                    val ktlintVersion = extension.version.get()
                    val ktlintSemver = SemVer.parse(ktlintVersion)
                    if (ktlintSemver >= SemVer(1, 0, 0)) {
                        // Baseline reporter maven coordinates changed in 1.0
                        listOf(
                            target.dependencies.create("com.pinterest.ktlint:ktlint-cli-reporter-baseline:$version"),
                            // this transitive dep was introduced in ktlint 1.0, but for some reason, it is not picked up automatically
                            target.dependencies.create("io.github.oshai:kotlin-logging:5.1.0")
                        )
                    } else if (SemVer.parse(ktlintVersion) >= SemVer(0, 41, 0)) {
                        // Baseline reporter is only available starting 0.41.0 release
                        listOf(target.dependencies.create("com.pinterest.ktlint:ktlint-reporter-baseline:$version"))
                    } else {
                        // Adding fake plain reporter as addLater() does not accept `null` value
                        // Generate baseline tasks anyway will not run on KtLint versions < 0.41.0
                        listOf(target.dependencies.create("com.pinterest.ktlint:ktlint-reporter-plain:$version"))
                    }
                }
            )
        )
        //      }
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
