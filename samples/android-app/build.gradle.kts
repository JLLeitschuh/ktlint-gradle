plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    compileSdkVersion(30)

    buildFeatures.viewBinding = true

    defaultConfig {
        minSdkVersion(23)
        targetSdkVersion(30)
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        val kotlinAdditionalSourceSets = project.file("src/main/kotlin")
        findByName("main")?.java?.srcDirs(kotlinAdditionalSourceSets)
    }
}

dependencies {
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.recycler.view)
    implementation(libs.android.material)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.espresso)
}

ktlint {
    android.set(true)
    outputColorName.set("RED")
}
