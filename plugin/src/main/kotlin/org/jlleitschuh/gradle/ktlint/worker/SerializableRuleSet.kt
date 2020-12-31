package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleSet
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.reflect.jvm.jvmName

/**
 * Special wrapper around [RuleSet] allowing to serialize/deserialize it into file.
 *
 * Should be removed once KtLint will add interface implementation into [RuleSet].
 */
internal class SerializableRuleSet(
    ruleSet: RuleSet
) : Serializable {
    @Transient
    var ruleSet: RuleSet = ruleSet
        private set

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.writeUTF(ruleSet.id)
        out.writeObject(
            ruleSet.rules.map { it::class.jvmName }
        )
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(oin: ObjectInputStream) {
        val id = oin.readUTF()
        @Suppress("UNCHECKED_CAST")
        val rulesMaps = oin.readObject() as List<String>
        val classLoader = this.javaClass.classLoader
        val rules = rulesMaps
            .map {
                val ruleClass = classLoader.loadClass(it)
                ruleClass.newInstance() as Rule
            }
        ruleSet = RuleSet(
            id,
            *rules.toTypedArray()
        )
    }

    companion object {
        private const val serialVersionUID: Long = 2012054L
    }
}
