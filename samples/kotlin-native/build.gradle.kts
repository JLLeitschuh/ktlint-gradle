import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins.apply("org.jlleitschuh.gradle.ktlint")
apply {
    plugin("org.jetbrains.kotlin.native")
}

configure<KtlintExtension> {
    verbose = true
    outputToConsole = true
}
