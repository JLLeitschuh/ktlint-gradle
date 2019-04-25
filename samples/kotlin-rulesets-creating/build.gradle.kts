plugins.apply("org.jlleitschuh.gradle.ktlint")
apply {
    plugin("kotlin")
}

dependencies {
    "compileOnly"(kotlin("stdlib"))
    "compileOnly"(kotlin("reflect"))
    "compileOnly"(kotlin("script-runtime"))
    "compileOnly"("com.pinterest.ktlint:ktlint-core:${SamplesVersions.ktlintCore}")
}
