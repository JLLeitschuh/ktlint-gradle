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
    kotlin("jvm") version "1.2.21" apply false
    id("com.android.application") version "3.0.1" apply false
    id("konan") version "0.6" apply false
}

allprojects {
    repositories {
        jcenter()
        google()
    }
}

task<Wrapper>("wrapper") {
    gradleVersion = "4.5"
    distributionType = Wrapper.DistributionType.ALL
}
