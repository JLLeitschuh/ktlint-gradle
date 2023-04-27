package org.jlleitschuh.gradle.ktlint.reporter

import com.pinterest.ktlint.core.Reporter
import org.jlleitschuh.gradle.ktlint.worker.SerializableLintError

/**
 * Abstraction over Reporter and ReporterV2
 */
interface GenericReporter<T> {
    fun beforeAll()

    /**
     * Called when file (matching the pattern) is found but before it's parsed.
     */
    fun before(file: String)
    fun onLintError(file: String, err: SerializableLintError, corrected: Boolean)

    /**
     * Called after ktlint is done with the file.
     */
    fun after(file: String)

    /**
     * Called once, after all the files (if any) have been processed.
     * It's guarantied to be called after all other [Reporter]s methods.
     */
    fun afterAll()
}
