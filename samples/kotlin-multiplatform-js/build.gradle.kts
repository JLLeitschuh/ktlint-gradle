import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins.apply("org.jlleitschuh.gradle.ktlint")
apply {
    plugin("kotlin-platform-js")
}

dependencies {
    "implementation"(kotlin("stdlib-js"))
    "expectedBy"(project(":samples:kotlin-multiplatform-common"))
}

configure<KtlintExtension> {
    verbose.set(true)
    outputToConsole.set(true)
}
