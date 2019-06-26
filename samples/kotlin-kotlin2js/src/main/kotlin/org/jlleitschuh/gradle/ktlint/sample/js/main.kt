package org.jlleitschuh.gradle.ktlint.sample.js

import kotlin.browser.document

val secondDiv: dynamic = document.createElement("div").apply {
    innerHTML = "<h1>Hello second time!</h1>"
}

fun main() {
    val firstDiv = document.createElement("div").apply { innerHTML = "<h1>Hello!</h1>" }
    document.body?.appendChild(firstDiv)

    document.body?.appendChild(secondDiv)
}
