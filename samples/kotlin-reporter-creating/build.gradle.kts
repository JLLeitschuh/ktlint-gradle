plugins {
    id("org.jlleitschuh.gradle.ktlint")
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("com.pinterest.ktlint:ktlint-core:${SamplesVersions.ktlintCore}")
}
