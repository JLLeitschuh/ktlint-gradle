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
    compileSdk = 33

    defaultConfig {
        minSdk = 23
        targetSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "beer"
    productFlavors {
        register("weissbier")
        register("kellerbier")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
