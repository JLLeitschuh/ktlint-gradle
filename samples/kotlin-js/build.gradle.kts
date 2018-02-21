import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

apply {
    plugins.apply("org.jlleitschuh.gradle.ktlint")
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
