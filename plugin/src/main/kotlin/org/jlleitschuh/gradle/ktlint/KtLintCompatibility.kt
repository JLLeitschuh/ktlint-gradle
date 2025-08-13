package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.jlleitschuh.gradle.ktlint.reporter.ReportersLoaderAdapter
import org.jlleitschuh.gradle.ktlint.reporter.ReportersProviderV2Loader
import org.jlleitschuh.gradle.ktlint.worker.KtLintInvocation100
import org.jlleitschuh.gradle.ktlint.worker.KtLintInvocationFactory
import java.io.Serializable

internal fun selectInvocation(version: String): KtLintInvocationFactory {
    val semVer = SemVer.parse(version)
    return if (semVer.major == 0) {
        throw GradleException("ktlint $version is incompatible with ktlint-gradle. Please upgrade to 1+")
    } else {
        KtLintInvocation100
    }
}

internal fun selectReportersLoaderAdapter(version: String): ReportersLoaderAdapter<*, out Serializable, *, *> {
    val semVer = SemVer.parse(version)
    return if (semVer.major == 0) {
        throw GradleException("ktlint $version is incompatible with ktlint-gradle. Please upgrade to 1+")
    } else {
        ReportersProviderV2Loader()
    }
}
