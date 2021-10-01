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
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.withType(Wrapper::class.java).configureEach {
    gradleVersion = libs.versions.gradleWrapper.get()
    distributionSha256Sum = libs.versions.gradleWrapperSha.get()
    distributionType = Wrapper.DistributionType.BIN
}
