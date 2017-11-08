plugins {
    kotlin("jvm") version "1.1.51"
    application
}

apply {
    plugin("kotlin")
}

application {
    mainClassName = "org.jlleitschuh.gradle.ktlint.sample.kotlin.MainKt"
}

dependencies {
    compile(kotlin("stdlib"))
}
