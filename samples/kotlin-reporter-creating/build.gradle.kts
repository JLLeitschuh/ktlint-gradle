plugins {
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.ktlint.core)
}
