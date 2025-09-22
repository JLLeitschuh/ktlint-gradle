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

internal fun createKtlintConfiguration(target: Project, extension: KtlintExtension): Configuration =
    target.configurations.maybeCreate(KTLINT_CONFIGURATION_NAME).apply {
        // Configurations in resolved state are not allowed to modify dependencies
        if (state != Configuration.State.UNRESOLVED) return@apply

        description = KTLINT_CONFIGURATION_DESCRIPTION

        isCanBeResolved = true
        isCanBeConsumed = false

        // Starting from KtLint 0.41.0 version published artifact has two variants: "external" and "shadowed"
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, target.objects.named(Bundling::class.java, Bundling.EXTERNAL))
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
                            // these transitive deps were introduced in ktlint 1.0, but for some reason not picked up automatically
                            target.dependencies.create("io.github.oshai:kotlin-logging:5.1.0"),
                            target.dependencies.create("io.github.detekt.sarif4k:sarif4k:0.5.0")
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

        shouldResolveConsistentlyWith(ktLintConfiguration)
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

        shouldResolveConsistentlyWith(ktLintConfiguration)

        withDependencies {
            // this transitive dep was introduced in ktlint 1.0, but for some reason, it is not picked up automatically
            dependencies.addAllLater(
                target.provider {
                    if (SemVer.parse(extension.version.get()) >= SemVer(1, 0, 0)) {
                        listOf(target.dependencies.create("io.github.detekt.sarif4k:sarif4k:0.5.0"))
                    } else {
                        listOf()
                    }
                }
            )

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

        shouldResolveConsistentlyWith(ktLintConfiguration)

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
                    } else {
                        // Baseline reporter is only available starting 0.41.0 release
                        listOf(target.dependencies.create("com.pinterest.ktlint:ktlint-reporter-baseline:$version"))
                    }
                }
            )
        )
        //      }
    }
