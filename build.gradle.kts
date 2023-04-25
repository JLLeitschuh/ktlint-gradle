buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") apply false
    id("com.android.application") apply false
    kotlin("js") apply false
    id("org.jlleitschuh.gradle.ktlint")
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.withType(Wrapper::class.java).configureEach {
    gradleVersion = pluginLibs.versions.gradleWrapper.get()
    distributionSha256Sum = pluginLibs.versions.gradleDistributionSha.get()
    distributionType = Wrapper.DistributionType.BIN
}
