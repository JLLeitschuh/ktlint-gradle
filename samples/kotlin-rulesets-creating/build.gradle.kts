plugins {
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

dependencies {
    compileOnly(pluginLibs.kotlin.reflect)
    compileOnly(pluginLibs.kotlin.script.runtime)
    compileOnly("com.pinterest.ktlint:ktlint-cli-ruleset-core:0.50.0")
}

ktlint {
    version = "1.0.1"
}
