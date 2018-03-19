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

    compileOptions {
        setSourceCompatibility(JavaVersion.VERSION_1_8)
        setTargetCompatibility(JavaVersion.VERSION_1_8)
    }
}

dependencies {
    "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jre7:${Versions.kotlin}")
    "implementation"("com.android.support:appcompat-v7:${Versions.androidSupport}")
    "implementation"("com.android.support:support-v4:${Versions.androidSupport}")
    "implementation"("com.android.support:recyclerview-v7:${Versions.androidSupport}")
    "implementation"("com.android.support:design:${Versions.androidSupport}")
    "testImplementation"("junit:junit:${Versions.junit}")
    "androidTestImplementation"("com.android.support.test:runner:${Versions.espressoRunner}")
    "androidTestImplementation"("com.android.support.test.espresso:espresso-core:${Versions.espresso}")
}

configure<KtlintExtension> {
    android = true
}
