import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins.apply("org.jlleitschuh.gradle.ktlint")

apply {
    plugin("kotlin-multiplatform")
    plugin("android-library")
}

configure<KotlinMultiplatformExtension> {
    targets.add(presets["android"].createTarget("androidLib"))

    sourceSets["commonMain"].apply {
        dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
        }
    }
    sourceSets["androidLibMain"].apply {
        dependsOn(sourceSets["commonMain"])

        dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        }
    }
}

configure<KtlintExtension> {
    verbose.set(true)
    outputToConsole.set(true)
}

configure<LibraryExtension> {
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
}
