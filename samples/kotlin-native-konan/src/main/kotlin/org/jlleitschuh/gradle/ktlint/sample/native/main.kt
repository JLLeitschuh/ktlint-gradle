package org.jlleitschuh.gradle.ktlint.sample.native

fun main(args: Array<String>) {
    (1..10).map { "Value: $it" }.forEach { println("Kotlin script: $it") }
}
