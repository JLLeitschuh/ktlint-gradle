import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    application
}

apply {
    plugins.apply("org.jlleitschuh.gradle.ktlint")
    plugin("kotlin")
}

application {
    mainClassName = "org.jlleitschuh.gradle.ktlint.sample.kotlin.MainKt"
}

dependencies {
    compile(kotlin("stdlib"))
}

configure<KtlintExtension> {
    verbose = true
    reporters = arrayOf(ReporterType.JSON)
}