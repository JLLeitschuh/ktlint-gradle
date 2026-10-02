package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer

/**
 * Known kotlin version that each KtLint release was built against. Add more as needed.
 */
internal val KTLINT_KOTLIN_COMPILER_VERSIONS: Map<String, String> = mapOf(
    "1.0.1" to "1.9.10",
    "1.5.0" to "2.1.0",
    "1.6.0" to "2.1.21",
    "1.7.1" to "2.2.0",
    "1.8.0" to "2.2.21"
)

/**
 * Returns the `org.jetbrains.kotlin` compiler version that the currently configured [KtlintExtension.version]
 * was built against.
 *
 * Some Gradle plugins (i.e. Spring Boot's dependency management plugin) apply project-wide version
 * constraints to every configuration, including the ones this plugin uses to resolve KtLint itself.
 * That can silently force KtLint onto a newer Kotlin compiler than it was built for.
 *
 * The simplest fix is to opt in to [KtlintExtension.enforceSupportedKotlinVersion], which applies this override
 * internally for every configuration this plugin resolves:
 *
 * ```kotlin
 * ktlint {
 *     enforceSupportedKotlinVersion.set(true)
 * }
 * ```
 *
 * If you need finer control or prefer not to expose a `ktlint` block, call this function directly instead:
 *
 * ```kotlin
 * val ktlintSupportedKotlinVersion = ktlint.getSupportedKotlinVersion()
 *
 * configurations.named("ktlint") {
 *     resolutionStrategy.eachDependency {
 *         if (requested.group == "org.jetbrains.kotlin") {
 *             useVersion(ktlintSupportedKotlinVersion)
 *         }
 *     }
 * }
 * ```
 *
 * @throws IllegalStateException if the configured KtLint version isn't in the known compatibility table.
 */
fun KtlintExtension.getSupportedKotlinVersion(): String =
    supportedKotlinCompilerVersionForOrNull(version.get())
        ?: error(
            "No known kotlin-compiler-embeddable version for KtLint ${version.get()}. " +
                "Known versions: ${KTLINT_KOTLIN_COMPILER_VERSIONS.keys.sortedBy { SemVer.parse(it) }}. " +
                "Please open an issue or PR at https://github.com/JLLeitschuh/ktlint-gradle to add it."
        )

internal fun supportedKotlinCompilerVersionForOrNull(ktlintVersion: String): String? =
    KTLINT_KOTLIN_COMPILER_VERSIONS[ktlintVersion]
