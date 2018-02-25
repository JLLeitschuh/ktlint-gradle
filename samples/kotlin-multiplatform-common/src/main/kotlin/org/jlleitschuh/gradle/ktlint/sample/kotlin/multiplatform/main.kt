package org.jlleitschuh.gradle.ktlint.sample.kotlin.multiplatform

/**
 * The main function is multi-platform, it depends on platform specific code.
 */
fun main(args: Array<String>) {
    println(Sample().doPlatformThing())
}
