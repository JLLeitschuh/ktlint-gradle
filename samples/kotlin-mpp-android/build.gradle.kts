plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    androidTarget()
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
}

android {
    namespace = "org.jlleitschuh"
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(23)
        targetSdkVersion(30)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions("beer")
    productFlavors {
        register("weissbier")
        register("kellerbier")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
