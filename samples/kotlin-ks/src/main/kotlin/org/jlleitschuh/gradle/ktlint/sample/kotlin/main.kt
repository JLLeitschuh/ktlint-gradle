package org.jlleitschuh.gradle.ktlint.sample.kotlin

fun main(args: Array<String>) {
    (1..10).map { "Value: $it" }.forEach { println("Kotlin script: $it") }
}
