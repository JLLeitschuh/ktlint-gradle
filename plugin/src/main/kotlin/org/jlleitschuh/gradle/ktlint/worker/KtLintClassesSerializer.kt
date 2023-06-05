package org.jlleitschuh.gradle.ktlint.worker

import org.apache.commons.io.serialization.ValidatingObjectInputStream
import org.gradle.api.GradleException
import java.io.File
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

    fun <T : Serializable> saveReporterProviders(
        reporterProviders: List<T>,
        serializedReporterProviders: File
    )

    companion object {
        fun create(): KtLintClassesSerializer =
            CurrentKtLintClassesSerializer()
    }
}

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
internal fun loadAnyErrors(file: File): List<LintErrorResult> {
    val errors = ValidatingObjectInputStream(
        file.inputStream().buffered()
    ).use {
        it.accept(
            ArrayList::class.java,
            LintErrorResult::class.java,
            SerializableLintError::class.java,
            File::class.java,
            Pair::class.java,
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
                else -> throw GradleException("invalid error format")
            }

        else -> throw GradleException("invalid error format")
    }
}
