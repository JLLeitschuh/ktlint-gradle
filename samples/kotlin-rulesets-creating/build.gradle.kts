plugins {
    `java`
}

plugins.apply("org.jlleitschuh.gradle.ktlint")
apply {
    plugin("kotlin")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("com.github.shyiko.ktlint:ktlint-core:0.22.0")

}
