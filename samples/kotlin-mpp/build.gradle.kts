plugins {
    application
    kotlin("multiplatform")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    jvm()
    js {
        nodejs()
    }
    linuxX64()

    sourceSets {
        val commonMain by getting
        val jvmMain by getting
        val jsMain by getting
        val linuxX64Main by getting
    }
}

application {
    mainClass.set("org.jlleitschuh.gradle.ktlint.sample.mpp.MainKt")
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
}
