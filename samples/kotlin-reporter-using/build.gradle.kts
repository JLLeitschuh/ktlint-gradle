import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    application
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

application {
    mainClass.set("org.jlleitschuh.gradle.ktlint.sample.kotlin.MainKt")
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
                dependency = projects.samples.kotlinReporterCreating
            }
        }
    }
}

tasks.named("loadKtlintReporters") {
    dependsOn(":samples:kotlin-rulesets-creating:build")
}

ktlint {
    version = "1.0.1"
}
