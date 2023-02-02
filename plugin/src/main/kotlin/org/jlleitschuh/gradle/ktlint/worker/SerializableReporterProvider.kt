package org.jlleitschuh.gradle.ktlint.worker

import com.pinterest.ktlint.core.ReporterProvider
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.reflect.jvm.jvmName

/**
 * Special wrapper around [ReporterProvider] allowing to serialize/deserialize it into file.
 *
 * Should be removed once KtLint will add interface implementation into [ReporterProvider].
 */
internal class SerializableReporterProvider(
    reporterProvider: ReporterProvider<*>
) : Serializable {
    @Transient
    var reporterProvider: ReporterProvider<*> = reporterProvider
        private set

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.writeUTF(reporterProvider::class.jvmName)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(oin: ObjectInputStream) {
        val reporterProviderClassName = oin.readUTF()
        val classLoader = this.javaClass.classLoader
        val reporterProviderClass = classLoader.loadClass(reporterProviderClassName)
        reporterProvider = reporterProviderClass.newInstance() as ReporterProvider<*>
    }

    companion object {
        private const val serialVersionUID: Long = 2012021L
    }
}
