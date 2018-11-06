package org.jlleitschuh.gradle.ktlint.sample.mpp

class JvmCommon : CommonInterface {
    override fun init() {
        println("Initializing ${JvmCommon::class.simpleName}")
    }

    override fun getName(): String = JvmCommon::class.simpleName ?: "Error"
}
