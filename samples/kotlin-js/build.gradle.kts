import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("js")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    js {
        nodejs()
    }
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)

    reporters {
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.JSON)
    }
}
