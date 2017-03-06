import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.1.0-rc-91"

    repositories {
        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap-1.1") }
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}

apply {
    plugin("kotlin")
    plugin("maven-publish")
}
group = "org.jlleitschuh.gradle"
version = "1.0-SNAPSHOT"

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