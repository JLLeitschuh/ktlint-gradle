import java.util.Properties

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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

include("plugin")
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
