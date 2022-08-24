pluginManagement {
    val latestRelease = file("VERSION_LATEST_RELEASE.txt").readText().trim()
    plugins {
        id("org.jlleitschuh.gradle.ktlint") version latestRelease
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
