plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("kotlin-platform-js")
}

dependencies {
    "implementation"(kotlin("stdlib-js"))
    "expectedBy"(project(":samples:kotlin-multiplatform-common"))
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
}
