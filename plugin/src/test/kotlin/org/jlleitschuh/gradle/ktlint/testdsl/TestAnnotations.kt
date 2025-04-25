package org.jlleitschuh.gradle.ktlint.testdsl

import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.KtlintPlugin
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream
import kotlin.streams.asStream

@Suppress("ConstPropertyName")
object TestVersions {
    const val minSupportedGradleVersion = "7.6.3" // lowest version for testing
    const val maxSupportedGradleVersion = "8.12.1"
    val pluginVersion = System.getProperty("project.version")
        ?: KtlintPlugin::class.java.`package`.implementationVersion
        ?: error("Unable to determine plugin version.")
    const val minSupportedKotlinPluginVersion = "1.4.32"
    const val maxSupportedKotlinPluginVersion = "2.1.10"
    const val minAgpVersion = "4.1.0"
    const val maxAgpVersion = "8.8.0"
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
            .filterIsInstance<GradleTestVersions>()
            .firstOrNull()
            ?: context.testClass.get().annotations.filterIsInstance<GradleTestVersions>().first()

        val minGradleVersion = if (getMajorJavaVersion() >= 21) {
            // Gradle 8.5 is needed to run on Java 21
            GradleVersion.version("8.5")
        } else {
            GradleVersion.version(versionsAnnotation.minVersion)
        }
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
