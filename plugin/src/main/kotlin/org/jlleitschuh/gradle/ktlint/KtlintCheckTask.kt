package org.jlleitschuh.gradle.ktlint

import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import java.io.PrintWriter
import javax.inject.Inject

@Suppress("UnstableApiUsage")
@CacheableTask
open class KtlintCheckTask @Inject constructor(
    objectFactory: ObjectFactory,
    projectLayout: ProjectLayout
) : BaseKtlintCheckTask(objectFactory, projectLayout) {
    override fun additionalConfig(): (PrintWriter) -> Unit = {}
}
