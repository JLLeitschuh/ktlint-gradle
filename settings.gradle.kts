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

include("samples:kotlin-ks")
include("samples:kotlin-gradle")
include("samples:android-app")

includeBuild("./plugin")
