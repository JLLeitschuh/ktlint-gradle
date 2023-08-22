package org.jlleitschuh.gradle.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.GradleException
import org.jlleitschuh.gradle.ktlint.reporter.ReportersLoaderAdapter
import org.jlleitschuh.gradle.ktlint.reporter.ReportersProviderLoader
import org.jlleitschuh.gradle.ktlint.reporter.ReportersProviderV2Loader
import org.jlleitschuh.gradle.ktlint.reporter.SerializableReportersProviderLoader
import org.jlleitschuh.gradle.ktlint.worker.BaselineLoader
import org.jlleitschuh.gradle.ktlint.worker.BaselineLoader45
import org.jlleitschuh.gradle.ktlint.worker.BaselineLoader46
import org.jlleitschuh.gradle.ktlint.worker.BaselineLoader47
import org.jlleitschuh.gradle.ktlint.worker.BaselineLoader48
import org.jlleitschuh.gradle.ktlint.worker.BaselineLoader49
import org.jlleitschuh.gradle.ktlint.worker.KtLintInvocation100
import org.jlleitschuh.gradle.ktlint.worker.KtLintInvocation45
import org.jlleitschuh.gradle.ktlint.worker.KtLintInvocation46
import org.jlleitschuh.gradle.ktlint.worker.KtLintInvocation47
import org.jlleitschuh.gradle.ktlint.worker.KtLintInvocation48
import org.jlleitschuh.gradle.ktlint.worker.KtLintInvocation49
import org.jlleitschuh.gradle.ktlint.worker.KtLintInvocation50
import org.jlleitschuh.gradle.ktlint.worker.KtLintInvocationFactory
import java.io.Serializable

internal fun selectInvocation(version: String): KtLintInvocationFactory {
    val semVer = SemVer.parse(version)
    return if (semVer.major == 0) {
        if (semVer.minor < 46) {
            KtLintInvocation45
        } else if (semVer.minor == 46) {
            KtLintInvocation46
        } else if (semVer.minor == 47) {
            KtLintInvocation47
        } else if (semVer.minor == 48) {
            KtLintInvocation48
        } else if (semVer.minor == 49) {
            KtLintInvocation49
        } else {
            KtLintInvocation50
        }
    } else {
        KtLintInvocation100
    }
}

internal fun selectBaselineLoader(version: String): BaselineLoader {
    val semVer = SemVer.parse(version)
    return if (semVer.major == 0) {
        if (semVer.minor < 46) {
            BaselineLoader45()
        } else if (semVer.minor == 46) {
            BaselineLoader46()
        } else if (semVer.minor == 47) {
            BaselineLoader47()
        } else if (semVer.minor == 48) {
            BaselineLoader48()
        } else if (semVer.minor == 49) {
            if (semVer.patch == 0) {
                throw GradleException("ktlint 0.49.0 is incompatible with ktlint-gradle. Please upgrade to 0.49.1+")
            } else {
                BaselineLoader49()
            }
        } else {
            BaselineLoader49()
        }
    } else {
        BaselineLoader49()
    }
}

internal fun selectReportersLoaderAdapter(version: String): ReportersLoaderAdapter<*, out Serializable, *, *> {
    val semVer = SemVer.parse(version)
    return if (semVer.major == 0) {
        if (semVer.minor < 41) {
            SerializableReportersProviderLoader()
        } else if (semVer.minor < 49) {
            ReportersProviderLoader()
        } else {
            ReportersProviderV2Loader()
        }
    } else {
        ReportersProviderV2Loader()
    }
}
