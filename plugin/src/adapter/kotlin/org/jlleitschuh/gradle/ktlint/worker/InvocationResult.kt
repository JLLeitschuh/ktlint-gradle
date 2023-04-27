package org.jlleitschuh.gradle.ktlint.worker

import java.io.Serializable

class FormatResult<T : Serializable>(
    val newCode: String,
    val errors: List<Pair<T, Boolean>>
)
