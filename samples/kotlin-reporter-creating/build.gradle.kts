plugins {
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

dependencies {
    compileOnly(pluginLibs.ktlint.core)
}
