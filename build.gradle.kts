buildscript {
    repositories {
        jcenter()
        google()
        maven("https://dl.bintray.com/jetbrains/kotlin-native-dependencies")
    }

    dependencies {
        classpath("org.jlleitschuh.gradle:ktlint-gradle:+")
    }
}

plugins {
    kotlin("jvm") version SamplesVersions.kotlin apply false
    id("com.android.application") version SamplesVersions.androidPlugin apply false
    id("konan") version SamplesVersions.kotlinNativePlugin apply false
}

allprojects {
    repositories {
        jcenter()
        google()
    }
}

apply {
    plugin("org.jlleitschuh.gradle.ktlint")
}

task<Wrapper>("wrapper") {
    gradleVersion = SamplesVersions.gradleWrapper
    distributionType = Wrapper.DistributionType.ALL
}
