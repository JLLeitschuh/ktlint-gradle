import java.util.Properties

pluginManagement {
    includeBuild("./plugin")

    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.5.21"
        id("org.jetbrains.kotlin.js") version "1.5.21"
    }

    repositories {
        gradlePluginPortal()
        google()
        maven("https://dl.bintray.com/jetbrains/kotlin-native-dependencies")
    }

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application" ->
                    useModule("com.android.tools.build:gradle:4.1.0")
            }
        }
    }
}

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    versionCatalogs {
        create("pluginLibs") {
            from(files("plugin/gradle/libs.versions.toml"))
        }
    }
}

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"

        publishAlways()
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
