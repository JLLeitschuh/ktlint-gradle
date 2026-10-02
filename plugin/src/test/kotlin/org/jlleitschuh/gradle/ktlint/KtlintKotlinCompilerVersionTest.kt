package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class KtlintKotlinCompilerVersionTest {

    @Test
    fun `returns the known kotlin-compiler-embeddable version for a known KtLint version`() {
        assertThat(supportedKotlinCompilerVersionForOrNull("1.8.0")).isEqualTo("2.2.21")
    }

    @Test
    fun `returns null for an unknown KtLint version`() {
        assertThat(supportedKotlinCompilerVersionForOrNull("0.1.0")).isNull()
    }

    @Test
    fun `every actively tested KtLint version has a known compiler version`() {
        assertThat(KTLINT_KOTLIN_COMPILER_VERSIONS.keys).containsAll(SUPPORTED_KTLINT_VERSIONS)
    }
}
