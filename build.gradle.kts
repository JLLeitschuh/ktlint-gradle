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
    kotlin("jvm") version Versions.kotlin apply false
    id("com.android.application") version Versions.androidPlugin apply false
    id("konan") version Versions.kotlinNativePlugin apply false
}

allprojects {
    repositories {
        jcenter()
        google()
    }
}

task<Wrapper>("wrapper") {
    gradleVersion = Versions.gradleWrapper
    distributionType = Wrapper.DistributionType.ALL
}
