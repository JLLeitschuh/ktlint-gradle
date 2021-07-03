plugins {
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

dependencies {
    compileOnly(pluginLibs.kotlin.reflect)
    compileOnly(pluginLibs.kotlin.script.runtime)
    compileOnly(pluginLibs.ktlint.core)
}
