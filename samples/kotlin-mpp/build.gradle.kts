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
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        val linuxX64Main by getting {}
    }
}

application {
    mainClassName = "org.jlleitschuh.gradle.ktlint.sample.mpp.MainKt"
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
}
