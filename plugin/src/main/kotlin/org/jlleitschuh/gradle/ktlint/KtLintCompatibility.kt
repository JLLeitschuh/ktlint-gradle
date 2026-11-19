package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.jlleitschuh.gradle.ktlint.baseline.BaselineAdapter
import org.jlleitschuh.gradle.ktlint.baseline.Ktlint1BaselineAdapter
import org.jlleitschuh.gradle.ktlint.baseline.Ktlint2BaselineAdapter
import org.jlleitschuh.gradle.ktlint.reporter.Ktlint2ReportersProviderV2Loader
import org.jlleitschuh.gradle.ktlint.reporter.ReportersLoaderAdapter
import org.jlleitschuh.gradle.ktlint.reporter.ReportersProviderV2Loader
import org.jlleitschuh.gradle.ktlint.worker.KtLintInvocation100
import org.jlleitschuh.gradle.ktlint.worker.KtLintInvocation2
import org.jlleitschuh.gradle.ktlint.worker.KtLintInvocationFactory
import java.io.Serializable

internal fun selectInvocation(version: String): KtLintInvocationFactory {
    val semVer = SemVer.parse(version)
    return when (semVer.major) {
        0 -> throw GradleException("ktlint $version is incompatible with ktlint-gradle. Please upgrade to 1+")
        1 -> KtLintInvocation100
        else -> KtLintInvocation2
    }
}

internal fun selectReportersLoaderAdapter(version: String): ReportersLoaderAdapter<*, out Serializable, *, *> {
    val semVer = SemVer.parse(version)
    return when (semVer.major) {
        0 -> throw GradleException("ktlint $version is incompatible with ktlint-gradle. Please upgrade to 1+")
        1 -> ReportersProviderV2Loader()
        else -> Ktlint2ReportersProviderV2Loader()
    }
}

internal fun selectBaselineAdapter(version: String): BaselineAdapter<*> {
    val semVer = SemVer.parse(version)
    return when (semVer.major) {
        0 -> throw GradleException("ktlint $version is incompatible with ktlint-gradle. Please upgrade to 1+")
        1 -> Ktlint1BaselineAdapter()
        else -> Ktlint2BaselineAdapter()
    }
}
