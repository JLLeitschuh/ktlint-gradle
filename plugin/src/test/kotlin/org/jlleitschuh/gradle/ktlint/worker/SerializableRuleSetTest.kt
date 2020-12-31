package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleSet
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

internal class SerializableRuleSetTest {
    private val ruleSet = RuleSet(
        "test",
        TestRule1(),
        TestRule2(),
    )

    @TempDir
    lateinit var temporaryFolder: File

    @Test
    internal fun `Should correctly serialize and deserialize RuleSet`() {
        val wrappedRuleSet = SerializableRuleSet(ruleSet)
        val serializeIntoFile = temporaryFolder.resolve("ktlint.test")

        ObjectOutputStream(FileOutputStream(serializeIntoFile)).use {
            it.writeObject(wrappedRuleSet)
        }

        ObjectInputStream(FileInputStream(serializeIntoFile)).use {
            val restoredRuleSet = it.readObject() as SerializableRuleSet
            assertThat(restoredRuleSet.ruleSet.id).isEqualTo(ruleSet.id)
            assertThat(restoredRuleSet.ruleSet.rules.size).isEqualTo(ruleSet.rules.size)
            assertThat(restoredRuleSet.ruleSet.rules[0]).isInstanceOf(TestRule1::class.java)
            assertThat(restoredRuleSet.ruleSet.rules[1]).isInstanceOf(TestRule2::class.java)
        }
    }

    class TestRule1 : Rule("one") {
        override fun visit(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
        ) = TODO("Not yet implemented")
    }

    class TestRule2 : Rule("two") {
        override fun visit(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
        ) = TODO("Not yet implemented")
    }
}
