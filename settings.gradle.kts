import java.util.Properties

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
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
include("samples:kotlin-multiplatform-common")

includeBuild("./plugin")
