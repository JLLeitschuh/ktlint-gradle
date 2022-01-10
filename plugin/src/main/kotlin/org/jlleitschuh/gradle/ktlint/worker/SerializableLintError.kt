package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.LintError
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * Special wrapper around [LintError] allowing to serialize/deserialize it into file.
 *
 * Should be removed once KtLint will add interface implementation into [LintError].
 */
internal class SerializableLintError(
    lintError: LintError
) : Serializable {

    @Transient
    var lintError: LintError = lintError
        private set

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.writeInt(lintError.line)
        out.writeInt(lintError.col)
        out.writeUTF(lintError.ruleId)
        out.writeUTF(lintError.detail)
        out.writeBoolean(lintError.canBeAutoCorrected)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(oin: ObjectInputStream) {
        val line = oin.readInt()
        val col = oin.readInt()
        val ruleId = oin.readUTF()
        val detail = oin.readUTF()
        val canBeAutocorrected = oin.readBoolean()

        lintError = LintError(
            line,
            col,
            ruleId,
            detail,
            canBeAutocorrected
        )
    }

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        lintError != (other as SerializableLintError).lintError -> false
        else -> true
    }

    override fun hashCode(): Int {
        return lintError.hashCode()
    }

    companion object {
        private const val serialVersionUID: Long = 20120922L
    }
}
