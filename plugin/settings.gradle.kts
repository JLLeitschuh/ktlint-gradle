pluginManagement {
    plugins {
        // We just always pull the latest published version of the plugin
        // Downside is that the build is now not 100% reproducible
        // Upside is that we don't need to update the version in the settings.gradle.kts
        // every release
        id("org.jlleitschuh.gradle.ktlint") version "latest.release"
        id("org.jetbrains.kotlin.jvm") version "2.1.20"
        id("com.gradle.plugin-publish") version "0.15.0"
        `java-gradle-plugin`
        id("com.github.johnrengelman.shadow") version "7.0.0"
        id("com.github.breadmoirai.github-release") version "2.5.2"
        id("com.netflix.nebula.release") version "20.2.0"
    }
}

plugins {
    id("com.gradle.develocity") version "3.17"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
    }
}

rootProject.name = "ktlint-gradle"
rootProject.buildFileName = "build.gradle.kts"
