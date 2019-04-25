import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    application
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

application {
    mainClassName = "org.jlleitschuh.gradle.ktlint.sample.kotlin.MainKt"
}

dependencies {
    compile(kotlin("stdlib"))
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    ruleSets.set(listOf("../kotlin-rulesets-creating/build/libs/kotlin-rulesets-creating.jar"))
    reporters.set(setOf(ReporterType.CHECKSTYLE, ReporterType.JSON))
}

tasks.findByName("ktlintMainSourceSetCheck")?.dependsOn(":samples:kotlin-rulesets-creating:build")
tasks.findByName("ktlintTestSourceSetCheck")?.dependsOn(":samples:kotlin-rulesets-creating:build")
tasks.findByName("ktlintKotlinScriptCheck")?.dependsOn(":samples:kotlin-rulesets-creating:build")
