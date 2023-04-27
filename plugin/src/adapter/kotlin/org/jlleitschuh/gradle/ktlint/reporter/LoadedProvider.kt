package org.jlleitschuh.gradle.ktlint.reporter

import java.io.Serializable

data class LoadedReporter(
    val reporterId: String,
    val fileExtension: String,
    val reporterOptions: Map<String, String>
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 201201233L
    }
}
