import org.jlleitschuh.gradle.ktlint.KtlintExtension

apply {
    plugins.apply("org.jlleitschuh.gradle.ktlint")
    plugin("kotlin-platform-js")
}

dependencies {
    "implementation"(kotlin("stdlib-js"))
    "expectedBy"(project(":samples:kotlin-multiplatform-common"))
}

configure<KtlintExtension> {
    verbose = true
    outputToConsole = true
}
