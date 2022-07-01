package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.ParseException
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import net.swiftzer.semver.SemVer
import org.apache.commons.io.input.MessageDigestCalculatingInputStream
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jlleitschuh.gradle.ktlint.worker.KtLintWorkAction.FormatTaskSnapshot.Companion.contentHash
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.ServiceLoader

@Suppress("UnstableApiUsage")
abstract class KtLintWorkAction : WorkAction<KtLintWorkAction.KtLintWorkParameters> {

    private val logger = Logging.getLogger("ktlint-worker")

    override fun execute() {
        val ruleSets = loadRuleSetsAndFilterThem(
            parameters.enableExperimental.getOrElse(false),
            parameters.disabledRules.getOrElse(emptySet())
        )

        val additionalEditorConfig = parameters
            .additionalEditorconfigFile
            .orNull
            ?.asFile
            ?.absolutePath
        val debug = parameters.debug.get()
        val formatSource = parameters.formatSource.getOrElse(false)

        resetEditorconfigCache()

        val result = mutableListOf<LintErrorResult>()
        val formattedFiles = mutableMapOf<File, ByteArray>()

        parameters.filesToLint.files.forEach {
            val errors = mutableListOf<Pair<LintError, Boolean>>()
            val ktLintParameters = KtLint.ExperimentalParams(
                fileName = it.absolutePath,
                text = it.readText(),
                ruleSets = ruleSets,
                debug = debug,
                editorConfigPath = additionalEditorConfig,
                script = !it.name.endsWith(".kt", ignoreCase = true),
                cb = { lintError, isCorrected ->
                    errors.add(lintError to isCorrected)
                }
            )

            try {
                if (formatSource) {
                    val currentFileContent = it.readText()
                    val updatedFileContent = KtLint.format(ktLintParameters)

                    if (updatedFileContent != currentFileContent) {
                        formattedFiles[it] = contentHash(it)
                        it.writeText(updatedFileContent)
                    }
                } else {
                    KtLint.lint(ktLintParameters)
                }
            } catch (e: ParseException) {
                throw GradleException(
                    "KtLint failed to parse file: ${it.absolutePath}",
                    e
                )
            }

            result.add(
                LintErrorResult(
                    lintedFile = it,
                    lintErrors = errors
                )
            )
        }

        KtLintClassesSerializer
            .create(
                SemVer.parse(parameters.ktLintVersion.get())
            )
            .saveErrors(
                result,
                parameters.discoveredErrorsFile.asFile.get()
            )

        if (formattedFiles.isNotEmpty()) {
            val snapshotFile = parameters.formatSnapshot.get().asFile
                .also { if (!it.exists()) it.createNewFile() }
            val snapshot = FormatTaskSnapshot(formattedFiles)
            FormatTaskSnapshot.writeIntoFile(snapshotFile, snapshot)
        }
    }

    private fun resetEditorconfigCache() {
        if (parameters.editorconfigFilesWereChanged.get()) {
            logger.info("Resetting KtLint caches")
            // Calling trimMemory() will also reset internal loaded `.editorconfig` cache
            KtLint.trimMemory()
        }
    }

    private fun generateUserData(): Map<String, String> {
        val userData = mutableMapOf(
            "android" to parameters.android.get().toString()
        )
        val disabledRules = parameters.disabledRules.get()
        if (disabledRules.isNotEmpty()) {
            userData["disabled_rules"] = disabledRules.joinToString(separator = ",")
        }

        return userData.toMap()
    }

    private fun loadRuleSetsAndFilterThem(
        enableExperimental: Boolean,
        disabledRules: Set<String>
    ): Set<RuleSet> = loadRuleSetsFromClasspath()
        .filterKeys { enableExperimental || it != "experimental" }
        .filterKeys { !(disabledRules.contains("standard") && it == "\u0000standard") }
        .toSortedMap()
        .mapValues { it.value.get() }
        .values
        .toSet()

    private fun loadRuleSetsFromClasspath(): Map<String, RuleSetProvider> = ServiceLoader
        .load(RuleSetProvider::class.java)
        .associateBy {
            val key = it.get().id
            // Adapted from KtLint CLI module
            if (key == "standard") "\u0000$key" else key
        }

    interface KtLintWorkParameters : WorkParameters {
        val filesToLint: ConfigurableFileCollection
        val android: Property<Boolean>
        val disabledRules: SetProperty<String>
        val enableExperimental: Property<Boolean>
        val debug: Property<Boolean>
        val additionalEditorconfigFile: RegularFileProperty
        val formatSource: Property<Boolean>
        val discoveredErrorsFile: RegularFileProperty
        val ktLintVersion: Property<String>
        val editorconfigFilesWereChanged: Property<Boolean>
        val formatSnapshot: RegularFileProperty
    }

    /**
     * Represents pre-formatted files snapshot (file + it contents hash).
     */
    internal class FormatTaskSnapshot(
        val formattedSources: Map<File, ByteArray>
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L

            fun readFromFile(snapshotFile: File) =
                ObjectInputStream(snapshotFile.inputStream().buffered())
                    .use {
                        it.readObject() as FormatTaskSnapshot
                    }

            fun writeIntoFile(
                snapshotFile: File,
                formatSnapshot: FormatTaskSnapshot
            ) = ObjectOutputStream(snapshotFile.outputStream().buffered())
                .use {
                    it.writeObject(formatSnapshot)
                }

            fun contentHash(file: File): ByteArray {
                return MessageDigestCalculatingInputStream(file.inputStream().buffered()).use {
                    it.readBytes()
                    it.messageDigest.digest()
                }
            }
        }
    }
}
