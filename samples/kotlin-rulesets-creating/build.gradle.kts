plugins.apply("org.jlleitschuh.gradle.ktlint")
apply {
    plugin("kotlin")
}

dependencies {
    "compileOnly"(kotlin("stdlib"))
    "compileOnly"("com.github.shyiko.ktlint:ktlint-core:${SamplesVersions.ktlintCore}")
}
