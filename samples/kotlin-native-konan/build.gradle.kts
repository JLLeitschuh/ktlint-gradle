import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jetbrains.kotlin.gradle.plugin.KonanArtifactContainer

plugins.apply("org.jlleitschuh.gradle.ktlint")
apply {
    plugin("konan")
}

configure<KonanArtifactContainer> {
    program("foo") {
        entryPoint("org.jlleitschuh.gradle.ktlint.sample.native.main")
    }
}

configure<KtlintExtension> {
    verbose = true
    outputToConsole = true
}
