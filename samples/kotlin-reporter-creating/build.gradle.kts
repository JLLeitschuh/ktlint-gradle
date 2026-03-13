plugins {
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

dependencies {
    compileOnly("com.pinterest.ktlint:ktlint-cli-reporter-core:1.8.0")
}

ktlint {
    version = "1.0.1"
}
