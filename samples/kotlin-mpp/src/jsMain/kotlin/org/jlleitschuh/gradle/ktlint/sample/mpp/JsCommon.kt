package org.jlleitschuh.gradle.ktlint.sample.mpp

class JsCommon : CommonInterface {
    override fun init() {
        println("Init")
    }

    override fun getName(): String = "JsCommon"
}
