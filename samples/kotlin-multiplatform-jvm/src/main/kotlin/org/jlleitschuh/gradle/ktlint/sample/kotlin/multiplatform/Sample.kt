package org.jlleitschuh.gradle.ktlint.sample.kotlin.multiplatform

/**
 * JVM platform implementation of demo class.
 */
actual class Sample {
    actual fun doPlatformThing(): CharSequence {
        return "hello from JVM platform"
    }
}
