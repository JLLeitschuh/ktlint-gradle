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
    reporters.set(setOf(
        ReporterType.CHECKSTYLE,
        ReporterType.JSON
    ))

    customReporters {
        reporter("csv", "csv", project(":samples:kotlin-reporter-creating"))
        reporter("html", "html", "me.cassiano:ktlint-html-reporter:0.2.3")
    }
}
