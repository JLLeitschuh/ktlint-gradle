package org.jlleitschuh.gradle.ktlint.sample.kotlin

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.KtStringTemplateEntry

class NoVarRule : Rule(RuleId("test:novar"), About()) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node is LeafPsiElement && node.textMatches("var") &&
            getNonStrictParentOfType(node, KtStringTemplateEntry::class.java) == null
        ) {
            emit(node.startOffset, "Unexpected var, use val instead", false)
        }
    }
}
