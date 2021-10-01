plugins {
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.kotlin.reflect)
    compileOnly(libs.kotlin.script.runtime)
    compileOnly(libs.ktlint.core)
}
