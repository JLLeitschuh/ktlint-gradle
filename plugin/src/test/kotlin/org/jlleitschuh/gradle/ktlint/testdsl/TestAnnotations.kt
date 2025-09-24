package org.jlleitschuh.gradle.ktlint.testdsl

import net.swiftzer.semver.SemVer
import org.gradle.util.GradleVersion
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
    const val minSupportedGradleVersion = "7.6.3" // lowest version for testing
    val maxSupportedGradleVersion =
        // gradle 9 requires Java 17
        if (SemVer.parse(getCurrentJavaVersion()).major >= 17) {
            "9.1.0"
        } else {
            "8.14.3"
        }
    val pluginVersion = System.getProperty("project.version")
        ?: KtlintPlugin::class.java.`package`.implementationVersion
        ?: error("Unable to determine plugin version.")
    const val minSupportedKotlinPluginVersion = "1.6.21"
    const val maxSupportedKotlinPluginVersion = "2.2.20"
    const val minAgpVersion = "4.1.0"
    const val maxAgpVersion = "8.8.0"
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GradleTestVersions(
    val minVersion: String = TestVersions.minSupportedGradleVersion,
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
