import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jetbrains.kotlin.gradle.plugin.KonanArtifactContainer
import org.jetbrains.kotlin.gradle.plugin.KonanProgram

plugins.apply("org.jlleitschuh.gradle.ktlint")
apply {
    plugin("konan")
}

configure<KonanArtifactContainer> {
    program("foo", Action {
        entryPoint("org.jlleitschuh.gradle.ktlint.sample.native.main")
    })
}

configure<KtlintExtension> {
    verbose.set(true)
    outputToConsole.set(true)
}
