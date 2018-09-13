import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins.apply("org.jlleitschuh.gradle.ktlint")
apply {
    plugin("kotlin-platform-jvm")
}

dependencies {
    "implementation"(kotlin("stdlib"))
    "expectedBy"(project(":samples:kotlin-multiplatform-common"))
}

configure<KtlintExtension> {
    verbose.set(true)
    outputToConsole = true
}
