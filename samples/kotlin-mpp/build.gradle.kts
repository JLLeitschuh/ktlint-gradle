import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        binaries {
            executable {
                mainClass.set("org.jlleitschuh.gradle.ktlint.sample.mpp.MainKt")
            }
        }
    }
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

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
}
