package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.ReporterProvider
import net.swiftzer.semver.SemVer
import org.apache.commons.io.serialization.ValidatingObjectInputStream
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

    fun saveReporterProviders(
        reporterProviders: List<ReporterProvider<*>>,
        serializedReporterProviders: File
    )

    fun loadReporterProviders(
        serializedReporterProviders: File
    ): List<ReporterProvider<*>>

    companion object {
        fun create(ktLintVersion: SemVer): KtLintClassesSerializer =
            if (ktLintVersion >= SemVer(0, 41, 0)) {
                CurrentKtLintClassesSerializer()
            } else {
                OldKtLintClassesSerializer()
            }
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

    override fun loadErrors(serializedErrors: File): List<LintErrorResult> = ValidatingObjectInputStream(
        serializedErrors.inputStream().buffered()
    ).use {
        it.accept(
            ArrayList::class.java,
            LintErrorResult::class.java,
            File::class.java,
            Pair::class.java,
            LintError::class.java,
            java.lang.Boolean::class.java
        )
        it.accept("kotlin.Pair")
        @Suppress("UNCHECKED_CAST")
        it.readObject() as List<LintErrorResult>
    }

    override fun saveReporterProviders(
        reporterProviders: List<ReporterProvider<*>>,
        serializedReporterProviders: File
    ) = ObjectOutputStream(
        serializedReporterProviders.outputStream().buffered()
    ).use { oos ->
        oos.writeObject(
            reporterProviders
        )
    }

    override fun loadReporterProviders(
        serializedReporterProviders: File
    ): List<ReporterProvider<*>> = ObjectInputStream(
        serializedReporterProviders.inputStream().buffered()
    ).use {
        @Suppress("UNCHECKED_CAST")
        it.readObject() as List<ReporterProvider<*>>
    }
}

/**
 * Should be used for pre '0.41.0' version when required KtLint classes does not implement [Serializable].
 */
private class OldKtLintClassesSerializer : KtLintClassesSerializer {
    override fun saveErrors(
        lintErrors: List<LintErrorResult>,
        serializedErrors: File
    ) = ObjectOutputStream(
        serializedErrors.outputStream().buffered()
    ).use {
        it.writeObject(lintErrors.map(LintErrorResultCompat::from))
    }

    override fun loadErrors(
        serializedErrors: File
    ): List<LintErrorResult> = ValidatingObjectInputStream(
        serializedErrors.inputStream().buffered()
    ).use {
        it.accept(
            ArrayList::class.java,
            LintErrorResultCompat::class.java,
            File::class.java,
            Pair::class.java,
            SerializableLintError::class.java,
            java.lang.Boolean::class.java
        )
        it.accept("kotlin.Pair")
        @Suppress("UNCHECKED_CAST")
        (it.readObject() as List<LintErrorResultCompat>).map(LintErrorResultCompat::to)
    }

    override fun saveReporterProviders(
        reporterProviders: List<ReporterProvider<*>>,
        serializedReporterProviders: File
    ) = ObjectOutputStream(
        serializedReporterProviders.outputStream().buffered()
    ).use { oos ->
        oos.writeObject(
            reporterProviders.map(::SerializableReporterProvider)
        )
    }

    override fun loadReporterProviders(
        serializedReporterProviders: File
    ): List<ReporterProvider<*>> = ValidatingObjectInputStream(
        serializedReporterProviders.inputStream().buffered()
    ).use {
        it.accept(
            ArrayList::class.java,
            SerializableReporterProvider::class.java
        )
        @Suppress("UNCHECKED_CAST")
        it.readObject() as List<SerializableReporterProvider>
    }.map { it.reporterProvider }

    private data class LintErrorResultCompat(
        val lintedFile: File,
        val lintErrors: List<Pair<SerializableLintError, Boolean>>
    ) : Serializable {
        fun to(): LintErrorResult = LintErrorResult(
            lintedFile,
            lintErrors.map { Pair(it.first.lintError, it.second) }
        )

        companion object {
            fun from(
                lintError: LintErrorResult
            ): LintErrorResultCompat = LintErrorResultCompat(
                lintError.lintedFile,
                lintError.lintErrors.map { Pair(SerializableLintError(it.first), it.second) }
            )
        }
    }
}
