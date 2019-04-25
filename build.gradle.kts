buildscript {
    repositories {
        google()
        jcenter()
        maven("https://dl.bintray.com/jetbrains/kotlin-native-dependencies")
    }

    dependencies {
        classpath("org.jlleitschuh.gradle:ktlint-gradle:+")
    }
}

plugins {
    kotlin("jvm") version SamplesVersions.kotlin apply false
    id("com.android.application") version SamplesVersions.androidPlugin apply false
    id("org.jetbrains.kotlin.konan") version SamplesVersions.kotlin apply false
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

apply {
    plugin("org.jlleitschuh.gradle.ktlint")
}

tasks.withType(Wrapper::class.java).configureEach {
    gradleVersion = SamplesVersions.gradleWrapper
    distributionType = Wrapper.DistributionType.ALL
}
