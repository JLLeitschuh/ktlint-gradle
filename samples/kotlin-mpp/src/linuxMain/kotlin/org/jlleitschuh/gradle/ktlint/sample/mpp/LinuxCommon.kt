package org.jlleitschuh.gradle.ktlint.sample.mpp

class LinuxCommon : CommonInterface {
    override fun init() {
        println("Init in linux")
    }

    override fun getName(): String = "LinuxCommon"
}
