import org.jetbrains.kotlin.gradle.plugin.experimental.internal.KotlinNativeMainComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.OutputKind

plugins {
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("native")
}

sourceSets {
    val main by getting {
        component(Action {
            with(this as KotlinNativeMainComponent) {
                outputKinds.set(listOf(OutputKind.EXECUTABLE))
                baseName.set("foo")
                extraOpts(listOf("-entry", "org.jlleitschuh.gradle.ktlint.sample.native.main"))
            }
        })
    }
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
}
