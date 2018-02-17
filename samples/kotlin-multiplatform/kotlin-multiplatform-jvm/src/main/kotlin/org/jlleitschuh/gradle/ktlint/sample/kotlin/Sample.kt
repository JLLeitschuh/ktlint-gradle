package org.jlleitschuh.gradle.ktlint.sample.kotlin

/**
 * JVM platform implementation of demo class.
 */
actual class Sample {
    actual fun doPlatformThing(): CharSequence {
        return "hello from JVM platform"
    }
}
