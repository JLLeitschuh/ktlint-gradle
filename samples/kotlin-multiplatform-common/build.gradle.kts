import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins.apply("org.jlleitschuh.gradle.ktlint")
apply {
    plugin("kotlin-platform-common")
}

dependencies {
    "implementation"(kotlin("stdlib-common"))
}

configure<KtlintExtension> {
    verbose.set(true)
    outputToConsole = true
}
