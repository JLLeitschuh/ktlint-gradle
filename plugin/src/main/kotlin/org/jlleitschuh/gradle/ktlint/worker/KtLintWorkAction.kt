package org.jlleitschuh.gradle.ktlint.worker

import net.swiftzer.semver.SemVer
import org.apache.commons.io.input.MessageDigestCalculatingInputStream
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jlleitschuh.gradle.ktlint.selectInvocation
import org.jlleitschuh.gradle.ktlint.worker.KtLintWorkAction.FormatTaskSnapshot.Companion.contentHash
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

@Suppress("UnstableApiUsage")
abstract class KtLintWorkAction : WorkAction<KtLintWorkAction.KtLintWorkParameters> {

    private val logger = Logging.getLogger("ktlint-worker")

    override fun execute() {
        val userData = generateUserData()
        val debug = parameters.debug.get()
        val formatSource = parameters.formatSource.getOrElse(false)
        val results = mutableListOf<LintErrorResult>()
        val formattedFiles = mutableMapOf<File, ByteArray>()
        if (parameters.additionalEditorconfig.isPresent &&
            parameters.additionalEditorconfig.get().isNotEmpty() &&
            parameters.ktLintVersion.map { SemVer.parse(it) }.get() < SemVer(0, 49)
        ) {
            logger.warn("additionalEditorconfig not supported until ktlint 0.49")
        }
        val ktlintInvoker: KtLintInvocation = when (
            val ktlintInvokerFactory = selectInvocation(parameters.ktLintVersion.get())
        ) {
            is KtLintInvocation47.Factory -> {
                ktlintInvokerFactory.initialize(
                    editorConfigPath = null,
                    userData = userData,
                    debug = debug,
                    parameters.enableExperimental.getOrElse(false),
                    parameters.disabledRules.getOrElse(emptySet())
                )
            }

            is KtLintInvocation48.Factory -> {
                ktlintInvokerFactory.initialize(
                    userData,
                    parameters.enableExperimental.getOrElse(false),
                    parameters.disabledRules.getOrElse(emptySet())
                )
            }

            is KtLintInvocation49.Factory -> {
                ktlintInvokerFactory.initialize(parameters.additionalEditorconfig.get())
            }

            is KtLintInvocation50.Factory -> {
                ktlintInvokerFactory.initialize(parameters.additionalEditorconfig.get())
            }

            is KtLintInvocation100.Factory -> {
                ktlintInvokerFactory.initialize(parameters.additionalEditorconfig.get())
            }

            else -> {
                throw GradleException("Incompatible ktlint version ${parameters.ktLintVersion}")
            }
        }

        resetEditorconfigCache(ktlintInvoker)

        parameters.filesToLint.files.forEach {
            try {
                if (formatSource) {
                    val currentFileContent = it.readText()
                    val result = ktlintInvoker.invokeFormat(it)
                    results.add(result.second)
                    val updatedFileContent = result.first

                    if (updatedFileContent != currentFileContent) {
                        formattedFiles[it] = contentHash(it)
                        it.writeText(updatedFileContent)
                    }
                } else {
                    val result = ktlintInvoker.invokeLint(it)
                    results.add(result)
                }
            } catch (e: RuntimeException) {
                logger.error(e.message)
                throw GradleException(
                    "KtLint failed to parse file: ${it.absolutePath}",
                    e
                )
            }
        }

        KtLintClassesSerializer
            .create()
            .saveErrors(
                results,
                parameters.discoveredErrorsFile.asFile.get()
            )

        if (formattedFiles.isNotEmpty()) {
            val snapshotFile = parameters.formatSnapshot.get().asFile
                .also { if (!it.exists()) it.createNewFile() }
            val snapshot = FormatTaskSnapshot(formattedFiles)
            FormatTaskSnapshot.writeIntoFile(snapshotFile, snapshot)
        }
    }

    private fun resetEditorconfigCache(ktLintInvocation: KtLintInvocation) {
        if (parameters.editorconfigFilesWereChanged.get()) {
            logger.info("Resetting KtLint caches")
            // Calling trimMemory() will also reset internal loaded `.editorconfig` cache
            ktLintInvocation.trimMemory()
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

    interface KtLintWorkParameters : WorkParameters {
        val filesToLint: ConfigurableFileCollection
        val android: Property<Boolean>
        val disabledRules: SetProperty<String>
        val enableExperimental: Property<Boolean>
        val debug: Property<Boolean>
        val additionalEditorconfig: MapProperty<String, String>
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
