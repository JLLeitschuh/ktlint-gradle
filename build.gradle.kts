buildscript {
    repositories {
        jcenter()
        google()
    }

    dependencies {
        classpath("org.jlleitschuh.gradle:ktlint-gradle:+")
        classpath("com.android.tools.build:gradle:3.0.1")
    }
}
plugins {
    kotlin(module = "jvm", version = "1.2.0") apply false
}

allprojects {
    repositories {
        jcenter()
        google()
    }
}

task<Wrapper>("wrapper") {
    gradleVersion = "4.2"
    distributionType = Wrapper.DistributionType.ALL
}
