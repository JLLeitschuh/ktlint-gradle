plugins {
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

dependencies {
    compileOnly(pluginLibs.kotlin.reflect)
    compileOnly(pluginLibs.kotlin.script.runtime)
    compileOnly("com.pinterest.ktlint:ktlint-cli-ruleset-core:1.0.1")
}

ktlint {
    version = "1.0.1"
}
