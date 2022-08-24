buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    alias(pluginLibs.plugins.kotlin.jvm) apply false
    alias(pluginLibs.plugins.kotlin.js) apply false
    id("com.android.application") apply false
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
    distributionSha256Sum = pluginLibs.versions.gradleWrapperSha.get()
    distributionType = Wrapper.DistributionType.BIN
}
