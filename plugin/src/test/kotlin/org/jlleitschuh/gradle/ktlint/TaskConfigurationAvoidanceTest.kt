package org.jlleitschuh.gradle.ktlint

import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.jlleitschuh.gradle.ktlint.tasks.LoadReportersTask
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.streams.asStream

class TaskConfigurationAvoidanceTest : AbstractPluginTest() {
    @BeforeEach
    internal fun setUp() {
        projectRoot.defaultProjectSetup()
    }

    @ParameterizedTest
    @MethodSource(value = ["org.jlleitschuh.gradle.ktlint.TaskConfigurationAvoidanceTest#pluginTaskNames"])
    internal fun checkTaskAvoidance(taskName: String) {
        //language=Groovy
        projectRoot.buildFile().appendText(
            """
            
            tasks
                 .withType(org.jlleitschuh.gradle.ktlint.tasks.$taskName.class)
                 .configureEach {
                      throw new RuntimeException("Created on configuration phase")
                 }
            """.trimIndent()
        )

        build("help", "-s")
    }

    companion object {
        @JvmStatic
        fun pluginTaskNames(): Stream<String> {
            return sequenceOf(
                LoadReportersTask::class.simpleName!!,
                GenerateReportsTask::class.simpleName!!,
                KtLintCheckTask::class.simpleName!!,
                KtLintFormatTask::class.simpleName!!
            ).asStream()
        }
    }
}
