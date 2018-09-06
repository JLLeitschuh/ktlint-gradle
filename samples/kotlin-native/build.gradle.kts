import org.jetbrains.kotlin.gradle.plugin.experimental.internal.KotlinNativeMainComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.OutputKind
import org.jetbrains.kotlin.gradle.plugin.experimental.plugins.kotlinNativeSourceSets
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins.apply("org.jlleitschuh.gradle.ktlint")
apply {
    plugin("org.jetbrains.kotlin.native")
}

kotlinNativeSourceSets["main"].component {
    // https://github.com/JetBrains/kotlin-native/issues/1807
    if (this is KotlinNativeMainComponent) {
        outputKinds.set(listOf(OutputKind.EXECUTABLE))
        baseName.set("foo")
        extraOpts(listOf("-entry", "org.jlleitschuh.gradle.ktlint.sample.native.main"))
    }
}

configure<KtlintExtension> {
    verbose = true
    outputToConsole = true
}
