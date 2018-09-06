import java.util.Properties

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven("https://dl.bintray.com/jetbrains/kotlin-native-dependencies")
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application" ->
                    useModule("com.android.tools.build:gradle:${requested.version}")
                "konan" ->
                    useModule("org.jetbrains.kotlin:kotlin-native-gradle-plugin:${requested.version}")
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
include("samples:kotlin-multiplatform-jvm")
include("samples:kotlin-multiplatform-js")
include("samples:kotlin-native")
include("samples:kotlin-native-konan")
include("samples:kotlin-rulesets-creating")
include("samples:kotlin-rulesets-using")

includeBuild("./plugin")
