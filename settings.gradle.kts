import java.util.Properties

pluginManagement {
    includeBuild("./plugin")

    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.1.20"
        id("org.jetbrains.kotlin.js") version "2.1.20"
        id("com.android.application") version "8.12.1"
    }

    repositories {
        gradlePluginPortal()
        google()
        maven("https://dl.bintray.com/jetbrains/kotlin-native-dependencies")
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    versionCatalogs {
        create("pluginLibs") {
            from(files("plugin/gradle/libs.versions.toml"))
        }
    }
}

plugins {
    id("com.gradle.develocity") version "3.17"
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
    }
}

rootProject.name = "ktlint-gradle-samples"

fun isAndroidSdkInLocalPropertiesSet(): Boolean {
    val propertiesFile = file("local.properties")
    if (propertiesFile.exists()) {
        val properties = Properties()
        properties.load(propertiesFile.inputStream())
        return properties.containsKey("sdk.dir")
    }

    return false
}

fun isAndroidSdkVariableSet(): Boolean = System.getenv().containsKey("ANDROID_HOME")

fun isAndroidSdkAvailable(): Boolean = isAndroidSdkVariableSet() || isAndroidSdkInLocalPropertiesSet()

include("samples:kotlin-ks")
include("samples:kotlin-gradle")
if (isAndroidSdkAvailable()) {
    include("samples:android-app")
}
include("samples:kotlin-js")
include("samples:kotlin-rulesets-creating")
include("samples:kotlin-rulesets-using")
include("samples:kotlin-reporter-creating")
include("samples:kotlin-reporter-using")
include("samples:kotlin-mpp")
if (isAndroidSdkAvailable()) {
    include("samples:kotlin-mpp-android")
}
