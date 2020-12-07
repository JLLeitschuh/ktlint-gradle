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
    implementation("androidx.fragment:fragment-ktx:${SamplesVersions.androidXFragment}")
    implementation("androidx.recyclerview:recyclerview:${SamplesVersions.androidXRecyclerView}")
    implementation("com.google.android.material:material:${SamplesVersions.androidMaterial}")

    testImplementation("junit:junit:${SamplesVersions.junit}")

    androidTestImplementation("androidx.test:runner:${SamplesVersions.androidXTestRunner}")
    androidTestImplementation(
        "androidx.test.espresso:espresso-core:${SamplesVersions.androidXEspresso}"
    )
}

ktlint {
    android.set(true)
    outputColorName.set("RED")
}
