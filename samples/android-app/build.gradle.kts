import com.android.build.gradle.AppExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

apply {
    plugin("android")
    plugin("kotlin-android")
    plugin("kotlin-android-extensions")
    plugins.apply("org.jlleitschuh.gradle.ktlint")
}

configure<AppExtension> {
    compileSdkVersion(27)

    defaultConfig {
        minSdkVersion(23)
        targetSdkVersion(27)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
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
        setSourceCompatibility(JavaVersion.VERSION_1_8)
        setTargetCompatibility(JavaVersion.VERSION_1_8)
    }

    sourceSets {
        val kotlinAddintionalSourceSets = project.file("src/main/kotlin")
        findByName("main")?.java?.srcDirs(kotlinAddintionalSourceSets)
    }
}

dependencies {
    "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jre7:${SamplesVersions.kotlin}")
    "implementation"("com.android.support:appcompat-v7:${SamplesVersions.androidSupport}")
    "implementation"("com.android.support:support-v4:${SamplesVersions.androidSupport}")
    "implementation"("com.android.support:recyclerview-v7:${SamplesVersions.androidSupport}")
    "implementation"("com.android.support:design:${SamplesVersions.androidSupport}")
    "testImplementation"("junit:junit:${SamplesVersions.junit}")
    "androidTestImplementation"("com.android.support.test:runner:${SamplesVersions.espressoRunner}")
    "androidTestImplementation"("com.android.support.test.espresso:espresso-core:${SamplesVersions.espresso}")
}

configure<KtlintExtension> {
    android.set(true)
}
