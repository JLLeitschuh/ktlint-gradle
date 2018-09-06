import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    application
}

plugins.apply("org.jlleitschuh.gradle.ktlint")
apply {
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
    outputToConsole = true
    ruleSets = arrayOf("../kotlin-rulesets-creating/build/libs/kotlin-rulesets-creating.jar")
    reporters = arrayOf(ReporterType.CHECKSTYLE, ReporterType.JSON)
}

afterEvaluate {
    tasks.findByName("ktlintMainCheck")?.dependsOn(":samples:kotlin-rulesets-creating:build")
    tasks.findByName("ktlintTestCheck")?.dependsOn(":samples:kotlin-rulesets-creating:build")
}
