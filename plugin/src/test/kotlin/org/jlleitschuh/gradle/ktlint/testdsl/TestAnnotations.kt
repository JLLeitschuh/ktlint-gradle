package org.jlleitschuh.gradle.ktlint.testdsl

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jlleitschuh.gradle.ktlint.KtlintBasePlugin
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream

object TestVersions {
    const val minSupportedGradleVersion = KtlintBasePlugin.LOWEST_SUPPORTED_GRADLE_VERSION
    const val maxSupportedGradleVersion = "8.1.1"
    val pluginVersion = File("VERSION_CURRENT.txt").readText().trim()
    const val minSupportedKotlinPluginVersion = "1.4.32"

    fun maxSupportedKotlinPluginVersion(
        gradleVersion: GradleVersion = GradleVersion.version(maxSupportedGradleVersion)
    ): String = when (gradleVersion) {
        in (GradleVersion.version("6.8.3")..GradleVersion.version(maxSupportedGradleVersion)) -> "1.8.10"
        in (GradleVersion.version("6.7.1")..GradleVersion.version("7.1.1")) -> "1.7.21"
        else -> minSupportedKotlinPluginVersion
    }
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GradleTestVersions(
    val minVersion: String = TestVersions.minSupportedGradleVersion,
    val maxVersion: String = TestVersions.maxSupportedGradleVersion,
    val additionalVersions: Array<String> = []
)

open class GradleArgumentsProvider : ArgumentsProvider {
    override fun provideArguments(
        context: ExtensionContext
    ): Stream<out Arguments> {
        return getGradleVersions(context)
            .map { Arguments.of(it) }
            .asStream()
    }

    fun getGradleVersions(
        context: ExtensionContext
    ): Sequence<GradleVersion> {
        val versionsAnnotation: GradleTestVersions = context
            .testMethod
            .get()
            .annotations
            .firstOrNull { it is GradleTestVersions }
            ?.let { it.cast() }
            ?: context.testClass.get().annotations.first { it is GradleTestVersions }.cast()

        val minGradleVersion = GradleVersion.version(versionsAnnotation.minVersion)
        val maxGradleVersion = GradleVersion.version(versionsAnnotation.maxVersion)
        val additionalGradleVersions = versionsAnnotation
            .additionalVersions
            .map(GradleVersion::version)

        additionalGradleVersions.forEach {
            assert(it in minGradleVersion..maxGradleVersion) {
                "Additional Gradle version ${it.version} should be between ${minGradleVersion.version} " +
                    "and ${maxGradleVersion.version}"
            }
        }

        return sequenceOf(minGradleVersion, *additionalGradleVersions.toTypedArray(), maxGradleVersion)
    }
}
