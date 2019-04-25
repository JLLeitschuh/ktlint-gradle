plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("kotlin-platform-jvm")
}

dependencies {
    "implementation"(kotlin("stdlib"))
    "expectedBy"(project(":samples:kotlin-multiplatform-common"))
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
}
