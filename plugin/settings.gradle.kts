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
