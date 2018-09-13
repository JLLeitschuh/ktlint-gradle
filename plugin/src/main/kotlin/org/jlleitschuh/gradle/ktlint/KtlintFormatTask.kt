package org.jlleitschuh.gradle.ktlint

import org.gradle.api.model.ObjectFactory
import org.gradle.process.JavaExecSpec
import javax.inject.Inject

open class KtlintFormatTask @Inject constructor(
    objectFactory: ObjectFactory
) : KtlintCheckTask(objectFactory) {
    override fun additionalConfig(): (JavaExecSpec) -> Unit = {
        it.args("-F")
    }
}
