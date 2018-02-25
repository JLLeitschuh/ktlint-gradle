import org.jlleitschuh.gradle.ktlint.KtlintExtension

apply {
    plugins.apply("org.jlleitschuh.gradle.ktlint")
    plugin("kotlin-platform-jvm")
}

dependencies {
    "implementation"(kotlin("stdlib"))
    "expectedBy"(project(":samples:kotlin-multiplatform-common"))
}

configure<KtlintExtension> {
    verbose = true
    outputToConsole = true
}
