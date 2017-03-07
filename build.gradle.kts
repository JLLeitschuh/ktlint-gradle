import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.1.0-rc-91"

    repositories {
        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap-1.1") }
        maven { setUrl("https://plugins.gradle.org/m2/") }
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("com.gradle.publish:plugin-publish-plugin:0.9.7")
    }
}

apply {
    plugin("kotlin")
    plugin("maven-publish")
    plugin("com.gradle.plugin-publish")
}
group = "org.jlleitschuh.gradle"
version = "1.0.1"

val kotlin_version: String by extra

repositories {
    maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap-1.1") }
    mavenCentral()
}

dependencies {
    compile(gradleApi())
    compile(gradleScriptKotlinApi())
    compile(kotlinModule("gradle-plugin"))
    compile("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlin_version")
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

/**
 * Retrieves or configures the [pluginBundle][com.gradle.publish.PluginBundleExtension] project extension.
 */
fun Project.pluginBundle(configure: com.gradle.publish.PluginBundleExtension.() -> Unit = {}) =
    extensions.getByName<com.gradle.publish.PluginBundleExtension>("pluginBundle").apply { configure() }
