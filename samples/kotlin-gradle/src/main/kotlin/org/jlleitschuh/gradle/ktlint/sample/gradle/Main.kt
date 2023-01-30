package org.jlleitschuh.gradle.ktlint.sample.gradle

fun main(args: Array<String>) {
    (1..10).map { "Value: $it" }.forEach { println("Gradle $it") }
}
