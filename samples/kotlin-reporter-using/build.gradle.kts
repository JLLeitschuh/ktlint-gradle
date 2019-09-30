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
    debug.set(true)
    outputToConsole.set(true)
    reporters {
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.JSON)

        customReporters {
            register("csv") {
                fileExtension = "csv"
                dependency = project(":samples:kotlin-reporter-creating")
            }

            register("html") {
                fileExtension = "html"
                dependency = "me.cassiano:ktlint-html-reporter:0.2.3"
            }
        }
    }
}
