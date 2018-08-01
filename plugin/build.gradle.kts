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
version = "4.2.0-SNAPSHOT"

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

/**
 * Configures the publishing environment for publishing with Travis CI.
 * All you need to do is push a tagged commit to github and Travis CI will automatically publish a
 * release of the plugin using the current [Project.getVersion].
 */
fun setupPublishingEnvironment() {
    val keyEnvironmentVariable = "GRADLE_PUBLISH_KEY"
    val secretEnvironmentVariable = "GRADLE_PUBLISH_SECRET"

    val keyProperty = "gradle.publish.key"
    val secretProperty = "gradle.publish.secret"

    if (System.getProperty(keyProperty) == null || System.getProperty(secretProperty) == null) {
        logger
            .info("`$keyProperty` or `$secretProperty` were not set. Attempting to configure from environment variables")

        val key: String? = System.getProperty(keyEnvironmentVariable)
        val secret: String? = System.getProperty(secretEnvironmentVariable)
        if (key != null && secret != null) {
            System.setProperty(keyProperty, key)
            System.setProperty(secretProperty, secret)
        } else {
            logger.debug("key or secret was null")
        }
    }
}

setupPublishingEnvironment()

gradlePlugin {
    (plugins) {
        "ktlintPlugin" {
            id = "org.jlleitschuh.gradle.ktlint"
            implementationClass = "org.jlleitschuh.gradle.ktlint.KtlintPlugin"
        }

        "ktlintBasePlugin" {
            id = "org.jlleitschuh.gradle.ktlint-base"
            implementationClass = "org.jlleitschuh.gradle.ktlint.KtlintBasePlugin"
        }

        "ktlintIdeaPlugin" {
            id = "org.jlleitschuh.gradle.ktlint-idea"
            implementationClass = "org.jlleitschuh.gradle.ktlint.KtlintIdeaPlugin"
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
        "ktlintIdeaPlugin" {
            id = "org.jlleitschuh.gradle.ktlint-idea"
            displayName = "Ktlint Gradle IntelliJ Configuration Plugin"
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
