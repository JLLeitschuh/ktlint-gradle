package org.jlleitschuh.gradle.ktlint.sample.mpp

fun main() {
    val common: CommonInterface = JvmCommon()
    common.init()
    println(common.getName())
}
