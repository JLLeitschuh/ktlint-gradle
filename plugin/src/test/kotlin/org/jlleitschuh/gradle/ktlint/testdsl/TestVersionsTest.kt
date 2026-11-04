package org.jlleitschuh.gradle.ktlint.testdsl

import org.gradle.util.GradleVersion
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestVersionsTest {
    @Test
    fun maxSupportedGradleVersionDoesNotExceedCurrentGradleVersion() {
        assertTrue(
            GradleVersion.version(TestVersions.maxSupportedGradleVersion) <= GradleVersion.current()
        )
    }
}
