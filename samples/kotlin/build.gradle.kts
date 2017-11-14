plugins {
    application
}

apply {
    plugins.apply("org.jlleitschuh.gradle.ktlint")
    plugin("kotlin")
}

application {
    mainClassName = "org.jlleitschuh.gradle.ktlint.sample.kotlin.MainKt"
}

dependencies {
    compile(kotlin("stdlib"))
}
