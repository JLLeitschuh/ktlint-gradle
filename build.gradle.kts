buildscript {
    repositories {
        google()
        jcenter()
        maven("https://dl.bintray.com/jetbrains/kotlin-native-dependencies")
    }
}

plugins {
    kotlin("jvm") version SamplesVersions.kotlin apply false
    id("com.android.application") version SamplesVersions.androidPlugin apply false
    id("org.jetbrains.kotlin.konan") version SamplesVersions.kotlin apply false
    kotlin("js") version SamplesVersions.kotlin apply false
    id("org.jlleitschuh.gradle.ktlint")
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks.withType(Wrapper::class.java).configureEach {
    gradleVersion = SamplesVersions.gradleWrapper
    distributionType = Wrapper.DistributionType.BIN
}
