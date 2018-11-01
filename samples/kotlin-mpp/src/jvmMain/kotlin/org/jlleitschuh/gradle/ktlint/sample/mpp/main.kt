package org.jlleitschuh.gradle.ktlint.sample.mpp

fun main(args: Array<String>) {
    val common: CommonInterface = JvmCommon()
    common.init()
    println(common.getName())
}
