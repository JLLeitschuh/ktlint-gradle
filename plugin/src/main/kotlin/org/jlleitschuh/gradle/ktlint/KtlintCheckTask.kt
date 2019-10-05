package org.jlleitschuh.gradle.ktlint

import java.io.PrintWriter
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask

@Suppress("UnstableApiUsage")
@CacheableTask
open class KtlintCheckTask @Inject constructor(
    objectFactory: ObjectFactory
) : BaseKtlintCheckTask(objectFactory) {
    override fun additionalConfig(): (PrintWriter) -> Unit = {}
}
