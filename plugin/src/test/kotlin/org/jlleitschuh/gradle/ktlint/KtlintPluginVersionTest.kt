package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.tasks.LoadReportersTask
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName
import java.io.File

@GradleTestVersions
class KtlintPluginVersionTest : AbstractPluginTest() {

    private fun File.useKtlintVersion(version: String) {
        //language=Groovy
        appendText(
            """
            buildDir = file("directory with spaces")

            ktlint {
                version = "$version"
            }
            """.trimIndent()
        )
    }
}
