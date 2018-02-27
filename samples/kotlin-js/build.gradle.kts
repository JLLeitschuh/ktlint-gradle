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
    verbose = true
    outputToConsole = true
    reporters = arrayOf(ReporterType.CHECKSTYLE, ReporterType.JSON)
}
