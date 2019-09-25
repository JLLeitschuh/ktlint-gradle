import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("js")
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    "implementation"(kotlin("stdlib-js"))
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)

    reporters {
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.JSON)
    }
}
