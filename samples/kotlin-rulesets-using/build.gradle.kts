import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    application
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

application {
    mainClass.set("org.jlleitschuh.gradle.ktlint.sample.kotlin.MainKt")
}

dependencies {
    ktlintRuleset(projects.samples.kotlinRulesetsCreating)
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    reporters {
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.JSON)
    }
}
