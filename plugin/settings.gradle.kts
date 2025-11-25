pluginManagement {
    plugins {
        // We just always pull the latest published version of the plugin
        // Downside is that the build is now not 100% reproducible
        // Upside is that we don't need to update the version in the settings.gradle.kts
        // every release
        id("org.jlleitschuh.gradle.ktlint") version "latest.release"
        id("org.jetbrains.kotlin.jvm") version "2.2.21"
        id("com.gradle.plugin-publish") version "2.0.0"
        `java-gradle-plugin`
        id("com.gradleup.shadow") version "8.3.9"
        id("com.github.breadmoirai.github-release") version "2.5.2"
        id("com.netflix.nebula.release") version "21.0.0"
    }
}

plugins {
    id("com.gradle.develocity") version "4.2.2"
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
