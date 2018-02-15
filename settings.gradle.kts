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

fun isAndroidSdkAvailable(): Boolean {
    val propertiesFile = file("local.properties")
    if (propertiesFile.exists()) {
        val properties = Properties()
        properties.load(propertiesFile.inputStream())
        return properties.containsKey("sdk.dir")
    }

    return false
}

include("samples:kotlin-ks")
include("samples:kotlin-gradle")
if (isAndroidSdkAvailable()) {
    include("samples:android-app")
}
includeBuild("./plugin")
