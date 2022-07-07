pluginManagement {
    val latestRelease = file("VERSION_LATEST_RELEASE.txt").readText().trim()
    plugins {
        id("org.jlleitschuh.gradle.ktlint") version latestRelease
        id("org.jetbrains.kotlin.jvm") version "1.5.31"
        id("com.gradle.plugin-publish") version "0.15.0"
        `java-gradle-plugin`
        `maven-publish`
        id("com.github.johnrengelman.shadow") version "7.0.0"
        id("com.github.breadmoirai.github-release") version "2.3.7"
        id("org.gradle.test-retry") version "1.3.1"
    }
}

enableFeaturePreview("VERSION_CATALOGS")

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"

        publishAlways()
    }
}

rootProject.name = "ktlint-gradle"
rootProject.buildFileName = "build.gradle.kts"
