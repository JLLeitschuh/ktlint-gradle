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
    val majorJavaVersion = SemVer.parse(getCurrentJavaVersion()).major
    val minSupportedGradleVersion = when (majorJavaVersion) {
        in Int.MIN_VALUE..20 -> "7.6.3"
        in 21..23 -> "8.5"
        in 24..Int.MAX_VALUE -> "9.1.0"
        else -> "7.6.3"
    }

    val maxSupportedGradleVersion = when (majorJavaVersion) {
        in Int.MIN_VALUE..16 -> "8.14.3" // gradle 9 requires Java 17
        in 17..Int.MAX_VALUE -> "9.1.0"
        else -> "9.1.0"
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
