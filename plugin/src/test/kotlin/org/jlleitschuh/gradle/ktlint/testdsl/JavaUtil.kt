package org.jlleitschuh.gradle.ktlint.testdsl

fun getMajorJavaVersion(): Int {
    val specVersion = System.getProperty("java.specification.version")
    return if (specVersion.startsWith("1.")) {
        specVersion.split(".")[1].toInt()
    } else {
        return specVersion.toInt()
    }
}
