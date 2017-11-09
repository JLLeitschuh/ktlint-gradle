allprojects {
    repositories {
        jcenter()
    }
}

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.1")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:+")
    }
}