import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.wrapper.Wrapper

plugins {
    kotlin("jvm") version PluginVersions.kotlin
    id("com.gradle.plugin-publish") version PluginVersions.gradlePublishPlugin
    id("maven-publish")
    id("org.jlleitschuh.gradle.ktlint") version PluginVersions.ktlintPlugin
}

group = "org.jlleitschuh.gradle"
version = "3.2.0"

repositories {
    jcenter()
    google()
    maven("https://dl.bintray.com/jetbrains/kotlin-native-dependencies")
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(kotlin("gradle-plugin", PluginVersions.kotlin))
    compileOnly("com.android.tools.build:gradle:${PluginVersions.androidPlugin}")
    compileOnly("org.jetbrains.kotlin:kotlin-native-gradle-plugin:${PluginVersions.kotlinNativePlugin}")
    compile("net.swiftzer.semver:semver:${PluginVersions.semver}")

    /*
     * Do not depend upon the gradle script kotlin plugin API. IE: gradleScriptKotlinApi()
     * It's currently in flux and has binary breaking changes in gradle 4.0
     * https://github.com/JLLeitschuh/ktlint-gradle/issues/9
     */
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJar") {
            from(components.getByName("java"))
        }
    }
}

pluginBundle {
    vcsUrl = "https://github.com/JLLeitschuh/ktlint-gradle"
    website = vcsUrl
    description = "Provides a convenient wrapper plugin over the ktlint project."
    tags = listOf("ktlint", "kotlin", "linting")

    (plugins) {
        "ktlintPlugin" {
            id = "org.jlleitschuh.gradle.ktlint"
            displayName = "Ktlint Gradle Plugin"
        }
    }
}

task<Wrapper>("wrapper") {
    gradleVersion = PluginVersions.gradleWrapper
}
