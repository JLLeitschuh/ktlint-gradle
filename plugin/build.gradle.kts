import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.wrapper.Wrapper

plugins {
    kotlin("jvm") version "1.2.0"
    id("com.gradle.plugin-publish") version "0.9.7"
    id("maven-publish")
    id("org.jlleitschuh.gradle.ktlint") version "2.2.1"
}

group = "org.jlleitschuh.gradle"
version = "3.0.0"

repositories {
    jcenter()
    google()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(kotlin("gradle-plugin", "1.2.0"))
    compileOnly("com.android.tools.build:gradle:3.0.0")
    compile("net.swiftzer.semver:semver:1.0.0")

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
    gradleVersion = "4.2"
}
