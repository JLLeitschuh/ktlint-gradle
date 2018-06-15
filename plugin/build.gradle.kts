import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.wrapper.Wrapper

plugins {
    kotlin("jvm") version PluginVersions.kotlin
    id("com.gradle.plugin-publish") version PluginVersions.gradlePublishPlugin
    id("java-gradle-plugin")
    id("maven-publish")
    id("org.jlleitschuh.gradle.ktlint") version PluginVersions.ktlintPlugin
}

group = "org.jlleitschuh.gradle"
version = "4.1.0"

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
    implementation("net.swiftzer.semver:semver:${PluginVersions.semver}")

    /*
     * Do not depend upon the gradle script kotlin plugin API. IE: gradleScriptKotlinApi()
     * It's currently in flux and has binary breaking changes in gradle 4.0
     * https://github.com/JLLeitschuh/ktlint-gradle/issues/9
     */

    testImplementation(gradleTestKit())
    testImplementation("junit:junit:${PluginVersions.junit}")
}

publishing {
    repositories {
        // Work around Gradle TestKit limitations in order to allow for compileOnly dependencies
        maven {
            name = "test"
            url = uri("$buildDir/plugin-test-repository")
        }
    }

    publications {
        create<MavenPublication>("mavenJar") {
            from(components.getByName("java"))
        }
    }
}

gradlePlugin {
    (plugins) {
        "ktlintPlugin" {
            id = "org.jlleitschuh.gradle.ktlint"
            implementationClass = "org.jlleitschuh.gradle.ktlint.KtlintPlugin"
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

tasks {
    val publishPluginsToTestRepository by creating {
        dependsOn("publishPluginMavenPublicationToTestRepository")
    }
    val processTestResources: ProcessResources by getting
    val writeTestProperties by creating(WriteProperties::class) {
        outputFile = processTestResources.destinationDir.resolve("test.properties")
        property("version", version)
        property("kotlinVersion", PluginVersions.kotlin)
    }
    processTestResources.dependsOn(writeTestProperties)
    "test" {
        dependsOn(publishPluginsToTestRepository)
    }
}
