import org.jlleitschuh.gradle.ktlint.KtlintExtension

apply {
    plugins.apply("org.jlleitschuh.gradle.ktlint")
    plugin("kotlin-platform-common")
}

dependencies {
    "implementation"(kotlin("stdlib-common"))
}

configure<KtlintExtension> {
    verbose = true
    outputToConsole = true
}
