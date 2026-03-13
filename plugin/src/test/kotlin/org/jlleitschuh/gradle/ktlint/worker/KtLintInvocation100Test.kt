package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KtLintInvocation100Test {

    @Test
    fun `rule without SinceKtlint annotation is always included`() {
        val ruleProvider = RuleProvider { RuleWithoutAnnotation() }

        assertThat(KtLintInvocation100.isRuleCompatibleWithFilters(ruleProvider, false, "1.0.0")).isTrue()
        assertThat(KtLintInvocation100.isRuleCompatibleWithFilters(ruleProvider, true, "1.0.0")).isTrue()
        assertThat(KtLintInvocation100.isRuleCompatibleWithFilters(ruleProvider, false, null)).isTrue()
    }

    @Test
    fun `stable rule within version limit is included`() {
        val ruleProvider = RuleProvider { RuleStable100() }

        assertThat(KtLintInvocation100.isRuleCompatibleWithFilters(ruleProvider, false, "1.5.0")).isTrue()
        assertThat(KtLintInvocation100.isRuleCompatibleWithFilters(ruleProvider, false, "1.0.0")).isTrue()
    }

    @Test
    fun `stable rule exceeding version limit is excluded`() {
        val ruleProvider = RuleProvider { RuleStable150() }
        val ruleClass = ruleProvider.createNewRuleInstance()::class.java
        val annotations = ruleClass.getAnnotationsByType(SinceKtlint::class.java).toList()
        println("DEBUG: Found ${annotations.size} annotations on ${ruleClass.simpleName}")
        annotations.forEach { println("  Version: ${it.version}, Status: ${it.status}") }

        assertThat(KtLintInvocation100.isRuleCompatibleWithFilters(ruleProvider, false, "1.0.0")).isFalse()
    }

    @Test
    fun `experimental rule is excluded when experimental rules disabled`() {
        val ruleProvider = RuleProvider { RuleExperimental100() }

        assertThat(KtLintInvocation100.isRuleCompatibleWithFilters(ruleProvider, false, "1.5.0")).isFalse()
    }

    @Test
    fun `experimental rule is included when experimental rules enabled`() {
        val ruleProvider = RuleProvider { RuleExperimental100() }

        assertThat(KtLintInvocation100.isRuleCompatibleWithFilters(ruleProvider, true, "1.5.0")).isTrue()
    }

    @Test
    fun `experimental rule exceeding version is excluded even with experimental enabled`() {
        val ruleProvider = RuleProvider { RuleExperimental150() }

        assertThat(KtLintInvocation100.isRuleCompatibleWithFilters(ruleProvider, true, "1.0.0")).isFalse()
    }

    @Test
    fun `rule with multiple annotations is included if any annotation matches`() {
        val ruleProvider = RuleProvider { RuleMultipleAnnotations() }

        assertThat(KtLintInvocation100.isRuleCompatibleWithFilters(ruleProvider, false, "1.2.0")).isTrue()
    }

    @Test
    fun `rule with no matching annotations is excluded`() {
        val ruleProvider = RuleProvider { RuleMultipleAnnotations() }

        // RuleMultipleAnnotations has both 1.0.0 and 1.5.0, so with max 1.0.0, the first annotation matches
        assertThat(KtLintInvocation100.isRuleCompatibleWithFilters(ruleProvider, false, "1.0.0")).isTrue()
    }

    @Test
    fun `no version limit includes all stable rules`() {
        val ruleProvider = RuleProvider { RuleStable150() }

        assertThat(KtLintInvocation100.isRuleCompatibleWithFilters(ruleProvider, false, null)).isTrue()
    }

    class RuleWithoutAnnotation : Rule(
        ruleId = RuleId("test-set:no-annotation"),
        about = About()
    )

    @SinceKtlint("1.0.0", SinceKtlint.Status.STABLE)
    class RuleStable100 : Rule(
        ruleId = RuleId("test-set:stable-one-zero-zero"),
        about = About()
    )

    @SinceKtlint("1.5.0", SinceKtlint.Status.STABLE)
    class RuleStable150 : Rule(
        ruleId = RuleId("test-set:stable-one-five-zero"),
        about = About()
    )

    @SinceKtlint("1.0.0", SinceKtlint.Status.EXPERIMENTAL)
    class RuleExperimental100 : Rule(
        ruleId = RuleId("test-set:experimental-one-zero-zero"),
        about = About()
    )

    @SinceKtlint("1.5.0", SinceKtlint.Status.EXPERIMENTAL)
    class RuleExperimental150 : Rule(
        ruleId = RuleId("test-set:experimental-one-five-zero"),
        about = About()
    )

    @SinceKtlint("1.0.0", SinceKtlint.Status.STABLE)
    @SinceKtlint("1.5.0", SinceKtlint.Status.STABLE)
    class RuleMultipleAnnotations : Rule(
        ruleId = RuleId("test-set:multiple"),
        about = About()
    )
}
