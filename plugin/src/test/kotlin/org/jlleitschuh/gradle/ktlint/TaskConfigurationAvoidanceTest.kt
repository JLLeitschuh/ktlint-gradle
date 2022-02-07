package org.jlleitschuh.gradle.ktlint

import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.jlleitschuh.gradle.ktlint.tasks.LoadReportersTask
import org.jlleitschuh.gradle.ktlint.testdsl.GradleArgumentsProvider
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream
import kotlin.streams.asStream

@GradleTestVersions
class TaskConfigurationAvoidanceTest : AbstractPluginTest() {

    @DisplayName("should support configuration avoidance")
    @ParameterizedTest(name = "{0} - task {1} {displayName}")
    @ArgumentsSource(PluginTasksNamesProvider::class)
    internal fun checkTaskAvoidance(
        gradleVersion: GradleVersion,
        taskName: String
    ) {
        project(gradleVersion) {
            //language=Groovy
            buildGradle.appendText(
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
    }

    class PluginTasksNamesProvider : GradleArgumentsProvider() {
        private val pluginTaskNames = listOf(
            LoadReportersTask::class.simpleName!!,
            GenerateReportsTask::class.simpleName!!,
            KtLintCheckTask::class.simpleName!!,
            KtLintFormatTask::class.simpleName!!
        )

        override fun provideArguments(
            context: ExtensionContext
        ): Stream<out Arguments> {
            return getGradleVersions(context)
                .flatMap { gradleVersion ->
                    pluginTaskNames.map { gradleVersion to it }.asSequence()
                }
                .map {
                    Arguments.of(it.first, it.second)
                }
                .asStream()
        }
    }
}
