plugins {
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

dependencies {
    "compileOnly"(kotlin("stdlib"))
    "compileOnly"(kotlin("reflect"))
    "compileOnly"(kotlin("script-runtime"))
    "compileOnly"("com.pinterest.ktlint:ktlint-core:${SamplesVersions.ktlintCore}")
}
