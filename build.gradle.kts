import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.wrapper.Wrapper

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.1.1"
    id("com.gradle.plugin-publish") version "0.9.7"
    id("maven-publish")
}

group = "org.jlleitschuh.gradle"
version = "2.0.1"

repositories {
    jcenter()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(kotlinModule("gradle-plugin", "1.1.1"))
    compileOnly("com.android.tools.build:gradle:2.3.2")
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
    gradleVersion = "3.5"
}

/**
 * Retrieves or configures the [pluginBundle][com.gradle.publish.PluginBundleExtension] project extension.
 */
fun Project.pluginBundle(configure: com.gradle.publish.PluginBundleExtension.() -> Unit = {}) =
    extensions.getByName<com.gradle.publish.PluginBundleExtension>("pluginBundle").apply { configure() }
