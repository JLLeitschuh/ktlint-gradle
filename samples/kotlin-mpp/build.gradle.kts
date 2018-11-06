import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins.apply("org.jlleitschuh.gradle.ktlint")
plugins {
    application
}

apply {
    plugin("kotlin-multiplatform")
}

configure<KotlinMultiplatformExtension> {
    targets.add(presets["jvmWithJava"].createTarget("jvm"))
    targets.add(presets["js"].createTarget("js"))
    targets.add(presets["linuxX64"].createTarget("linux"))

    sourceSets["commonMain"].apply {
        dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
        }
    }
    sourceSets["jvmMain"].apply {
        dependsOn(sourceSets["commonMain"])

        dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        }
    }
    sourceSets["jsMain"].apply {
        dependsOn(sourceSets["commonMain"])

        dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
        }
    }
    sourceSets["linuxMain"].apply {
        dependsOn(sourceSets["commonMain"])
    }
}

application {
    mainClassName = "org.jlleitschuh.gradle.ktlint.sample.mpp.MainKt"
}

configure<KtlintExtension> {
    verbose.set(true)
    outputToConsole.set(true)
}
