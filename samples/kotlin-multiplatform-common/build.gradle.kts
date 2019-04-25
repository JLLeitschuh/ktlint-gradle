plugins {
    id("kotlin-platform-common")
    id("org.jlleitschuh.gradle.ktlint")
}

dependencies {
    "implementation"(kotlin("stdlib-common"))
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
}
