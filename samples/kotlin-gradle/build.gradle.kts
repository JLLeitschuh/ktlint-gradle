import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm")
    application
    id("org.jlleitschuh.gradle.ktlint")
}

application {
    mainClass.set("org.jlleitschuh.gradle.ktlint.sample.gradle.MainKt")
}

ktlint {
    debug = true
    verbose = true
    reporters {
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.PLAIN)

        customReporters {
            create("html") {
                fileExtension = "csv"
                dependency = project.dependencies.project(":samples:kotlin-reporter-creating")
            }
        }
    }
}
