buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version SamplesVersions.kotlin apply false
    id("com.android.application") version SamplesVersions.androidPlugin apply false
    kotlin("js") version SamplesVersions.kotlin apply false
    id("org.jlleitschuh.gradle.ktlint")
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.withType(Wrapper::class.java).configureEach {
    gradleVersion = SamplesVersions.gradleWrapper
    distributionSha256Sum = SamplesVersions.gradleWrapperSha
    distributionType = Wrapper.DistributionType.BIN
}
