package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.ReporterProvider
import net.swiftzer.semver.SemVer
import org.apache.commons.io.serialization.ValidatingObjectInputStream
import org.gradle.api.GradleException
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

internal interface KtLintClassesSerializer {
    fun saveErrors(
        lintErrors: List<LintErrorResult>,
        serializedErrors: File
    )

    fun loadErrors(
        serializedErrors: File
    ): List<LintErrorResult>

    fun <T : Serializable>saveReporterProviders(
        reporterProviders: List<T>,
        serializedReporterProviders: File
    )

    companion object {
        fun create(ktLintVersion: SemVer): KtLintClassesSerializer =
            CurrentKtLintClassesSerializer()
    }
}

/**
 * Should be used for KtLint '0.41.0'+ versions.
 */
private class CurrentKtLintClassesSerializer : KtLintClassesSerializer {
    override fun saveErrors(
        lintErrors: List<LintErrorResult>,
        serializedErrors: File
    ) = ObjectOutputStream(serializedErrors.outputStream().buffered()).use {
        it.writeObject(lintErrors)
    }

    override fun loadErrors(serializedErrors: File): List<LintErrorResult> = loadAnyErrors(serializedErrors)

    override fun <T : Serializable> saveReporterProviders(
        reporterProviders: List<T>,
        serializedReporterProviders: File
    ) = ObjectOutputStream(
        serializedReporterProviders.outputStream().buffered()
    ).use { oos ->
        oos.writeObject(
            reporterProviders
        )
    }
}

@Suppress("UNCHECKED_CAST")
fun loadAnyErrors(file: File): List<LintErrorResult> {
    val errors = ValidatingObjectInputStream(
        file.inputStream().buffered()
    ).use {
        it.accept(
            ArrayList::class.java,
            SerializableLintError::class.java,
            LintErrorResult::class.java,
            LegacyLintErrorResult::class.java,
            File::class.java,
            Pair::class.java,
            LintError::class.java,
            java.lang.Boolean::class.java
        )
        it.accept("kotlin.Pair")
        it.readObject()
    }
    return when (errors) {
        is List<*> ->
            when (errors.first()) {
                null -> emptyList()
                is LintErrorResult -> errors as List<LintErrorResult>
                // handle files created with old ktlint-gradle
                is LegacyLintErrorResult -> (errors as List<LegacyLintErrorResult>).map { it.toNew() }
                else -> throw GradleException("invalid error format")
            }
        else -> throw GradleException("invalid error format")
    }
}
