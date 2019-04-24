plugins {
    kotlin("jvm") version PluginVersions.kotlin
    id("com.gradle.plugin-publish") version PluginVersions.gradlePublishPlugin
    `java-gradle-plugin`
    `maven-publish`
    id("org.jlleitschuh.gradle.ktlint") version PluginVersions.ktlintPlugin
}

group = "org.jlleitschuh.gradle"
version = "7.5.0-SNAPSHOT"

repositories {
    google()
    jcenter()
    maven("https://dl.bintray.com/jetbrains/kotlin-native-dependencies")
}

tasks.withType<PluginUnderTestMetadata>().configureEach {
    pluginClasspath.from(configurations.compileOnly)
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(kotlin("gradle-plugin", PluginVersions.kotlin))
    compileOnly("com.android.tools.build:gradle:${PluginVersions.androidPlugin}")
    compileOnly("org.jetbrains.kotlin:kotlin-native-gradle-plugin:${PluginVersions.kotlin}")
    implementation("net.swiftzer.semver:semver:${PluginVersions.semver}")

    /*
     * Do not depend upon the gradle script kotlin plugin API. IE: gradleScriptKotlinApi()
     * It's currently in flux and has binary breaking changes in gradle 4.0
     * https://github.com/JLLeitschuh/ktlint-gradle/issues/9
     */

    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:${PluginVersions.junit5}")
    testImplementation("org.assertj:assertj-core:${PluginVersions.assertJ}")
    testImplementation(kotlin("reflect"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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

        val key: String? = System.getenv(keyEnvironmentVariable)
        val secret: String? = System.getenv(secretEnvironmentVariable)
        if (key != null && secret != null) {
            System.setProperty(keyProperty, key)
            System.setProperty(secretProperty, secret)
        } else {
            logger.warn("key or secret was null")
        }
    }
}

setupPublishingEnvironment()

gradlePlugin {
    (plugins) {
        register("ktlintPlugin") {
            id = "org.jlleitschuh.gradle.ktlint"
            implementationClass = "org.jlleitschuh.gradle.ktlint.KtlintPlugin"
        }
        register("ktlintIdeaPlugin") {
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
            displayName = "Ktlint Gradle Plugin"
        }
        "ktlintIdeaPlugin" {
            displayName = "Ktlint Gradle IntelliJ Configuration Plugin"
        }
    }
}

tasks.withType(Wrapper::class.java).configureEach {
    gradleVersion = PluginVersions.gradleWrapper
}
