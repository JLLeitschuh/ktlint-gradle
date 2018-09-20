import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins.apply("org.jlleitschuh.gradle.ktlint")
apply {
    plugin("kotlin2js")
}

dependencies {
    "implementation"(kotlin("stdlib-js"))
}

configure<KtlintExtension> {
    verbose.set(true)
    outputToConsole.set(true)
    reporters.set(setOf(ReporterType.CHECKSTYLE, ReporterType.JSON))
}
