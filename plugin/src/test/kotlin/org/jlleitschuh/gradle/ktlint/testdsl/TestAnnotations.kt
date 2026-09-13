package org.jlleitschuh.gradle.ktlint.testdsl

import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.KtlintBasePlugin.Companion.LOWEST_SUPPORTED_GRADLE_VERSION
import org.jlleitschuh.gradle.ktlint.KtlintPlugin
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream
import kotlin.streams.asStream

fun getCurrentJavaVersion(): String {
    return Runtime::class.java.getPackage().specificationVersion // java 8
        ?: Runtime::class.java.getMethod("version").invoke(null).toString() // java 9+
}

@Suppress("ConstPropertyName")
object TestVersions {
    val majorJavaVersion = getCurrentJavaVersion().substringBefore(".").toInt()
    val minSupportedGradleVersion = when (majorJavaVersion) {
        in 24..Int.MAX_VALUE -> "9.1.0"
        else -> LOWEST_SUPPORTED_GRADLE_VERSION
    }

    val maxSupportedGradleVersion = when (majorJavaVersion) {
        in Int.MIN_VALUE..16 -> "8.14.3" // gradle 9 requires Java 17
        else -> "9.8.2"
    }

    val pluginVersion = System.getProperty("project.version")
        ?: KtlintPlugin::class.java.`package`.implementationVersion
        ?: error("Unable to determine plugin version.")
    const val minSupportedKotlinPluginVersion = "1.9.25"
    const val maxSupportedKotlinPluginVersion = "2.4.21"
    const val minAgpVersion = "4.1.0"
    const val maxAgpVersion = "9.0.0"
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GradleTestVersions(
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
            .filterIsInstance<GradleTestVersions>()
            .firstOrNull()
            ?: context.testClass.get().annotations.filterIsInstance<GradleTestVersions>().first()
        val minGradleVersion = GradleVersion.version(TestVersions.minSupportedGradleVersion)
        val maxGradleVersion = GradleVersion.version(TestVersions.maxSupportedGradleVersion)
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

class SupportedKtlintVersionsProvider : GradleArgumentsProvider() {
    val majorJavaVersion = getCurrentJavaVersion().substringBefore(".").toInt()
    val supportedKtlintVersions = when (majorJavaVersion) {
        in Int.MIN_VALUE..16 -> listOf(
            "1.0.1", // lowest
            "1.8.0", // default, and highest version that runs on Java < 17
        )

        else -> listOf(
            "1.0.1", // lowest
            "1.8.0", // default
            "2.0.0-ALPHA-4" // highest
        )
    }

    override fun provideArguments(
        context: ExtensionContext
    ): Stream<out Arguments> {
        // Using multiple Gradle versions produces OOM exceptions on Windows OS
        return sequenceOf(GradleVersion.version(TestVersions.maxSupportedGradleVersion))
            .flatMap { gradleVersion ->
                supportedKtlintVersions.map { gradleVersion to it }.asSequence()
            }
            .map {
                Arguments.of(it.first, it.second)
            }
            .asStream()
    }
}
