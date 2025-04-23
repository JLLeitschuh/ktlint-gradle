pluginManagement {
    val latestRelease = file("VERSION_LATEST_RELEASE.txt").readText().trim()
    plugins {
        id("org.jlleitschuh.gradle.ktlint") version latestRelease
        id("org.jetbrains.kotlin.jvm") version "2.1.20"
        id("com.gradle.plugin-publish") version "0.15.0"
        `java-gradle-plugin`
        `maven-publish`
        id("com.github.johnrengelman.shadow") version "7.0.0"
        id("com.github.breadmoirai.github-release") version "2.3.7"
    }
}

plugins {
    id("com.gradle.develocity") version "3.17"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
    }
}

rootProject.name = "ktlint-gradle"
rootProject.buildFileName = "build.gradle.kts"
