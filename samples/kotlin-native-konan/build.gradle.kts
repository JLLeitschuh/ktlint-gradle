import org.jetbrains.kotlin.gradle.plugin.konan.KonanArtifactContainer

plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("konan")
}

configure<KonanArtifactContainer> {
    program("foo", Action {
        entryPoint("org.jlleitschuh.gradle.ktlint.sample.native.main")
    })
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
}
