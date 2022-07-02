package org.jlleitschuh.gradle.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jlleitschuh.gradle.ktlint.tasks.GenerateBaselineTask
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.jlleitschuh.gradle.ktlint.testdsl.CommonTest
import org.jlleitschuh.gradle.ktlint.testdsl.GradleTestVersions
import org.jlleitschuh.gradle.ktlint.testdsl.TestProject.Companion.FAIL_SOURCE_FILE
import org.jlleitschuh.gradle.ktlint.testdsl.build
import org.jlleitschuh.gradle.ktlint.testdsl.buildAndFail
import org.jlleitschuh.gradle.ktlint.testdsl.project
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File

@GradleTestVersions
class KtlintPluginTest : AbstractPluginTest() {

    @DisplayName("Should fail on failing sources")
    @CommonTest
    fun failOnStyleViolation(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withFailingSources()

            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
                assertThat(output).contains("Unnecessary space(s)")
            }
        }
    }

    @DisplayName("Should succeed check on clean sources")
    @CommonTest
    fun passCleanSources(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    @DisplayName("Should generate code style files in project")
    @CommonTest
    fun generateIdeaCodeStyle(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            val ideaRootDir = projectPath.resolve(".idea").apply { mkdir() }

            build(APPLY_TO_IDEA_TASK_NAME) {
                assertThat(task(":$APPLY_TO_IDEA_TASK_NAME")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(ideaRootDir.listFiles()?.isNullOrEmpty()).isFalse()
            }
        }
    }

    @DisplayName("Should generate code style file globally")
    @CommonTest
    fun generateIdeaCodeStyleGlobally(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val ideaRootDir = projectPath.resolve(".idea").apply { mkdir() }

            build(APPLY_TO_IDEA_GLOBALLY_TASK_NAME) {
                assertThat(task(":$APPLY_TO_IDEA_GLOBALLY_TASK_NAME")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(ideaRootDir.listFiles()?.isNullOrEmpty()).isFalse()
            }
        }
    }

    @DisplayName("Should show only plugin meta tasks in task output")
    @CommonTest
    fun showOnlyMetaTasks(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()

            build("tasks") {
                val ktlintTasks = output
                    .lineSequence()
                    .filter { it.startsWith("ktlint", ignoreCase = true) }
                    .toList()

                assertThat(ktlintTasks).hasSize(5)
                assertThat(ktlintTasks).anyMatch { it.startsWith(CHECK_PARENT_TASK_NAME) }
                assertThat(ktlintTasks).anyMatch { it.startsWith(FORMAT_PARENT_TASK_NAME) }
                assertThat(ktlintTasks).anyMatch { it.startsWith(APPLY_TO_IDEA_TASK_NAME) }
                assertThat(ktlintTasks).anyMatch { it.startsWith(APPLY_TO_IDEA_GLOBALLY_TASK_NAME) }
                assertThat(ktlintTasks).anyMatch { it.startsWith(GenerateBaselineTask.NAME) }
            }
        }
    }

    @DisplayName("Should show all KtLint tasks in task output")
    @CommonTest
    fun allKtlintTasks(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            build("tasks", "--all") {
                val ktlintTasks = output
                    .lineSequence()
                    .filter { it.startsWith("ktlint", ignoreCase = true) }
                    .toList()

                // Plus for main and test sources format and check tasks
                // Plus two kotlin script tasks
                assertThat(ktlintTasks).hasSize(11)
                assertThat(ktlintTasks).anyMatch { it.startsWith(CHECK_PARENT_TASK_NAME) }
                assertThat(ktlintTasks).anyMatch { it.startsWith(FORMAT_PARENT_TASK_NAME) }
                assertThat(ktlintTasks).anyMatch { it.startsWith(APPLY_TO_IDEA_TASK_NAME) }
                assertThat(ktlintTasks).anyMatch { it.startsWith(APPLY_TO_IDEA_GLOBALLY_TASK_NAME) }
                assertThat(ktlintTasks).anyMatch { it.startsWith(kotlinScriptCheckTaskName) }
                assertThat(ktlintTasks).anyMatch {
                    it.startsWith(
                        GenerateReportsTask.generateNameForKotlinScripts(GenerateReportsTask.LintType.FORMAT)
                    )
                }
                assertThat(ktlintTasks).anyMatch { it.startsWith(GenerateBaselineTask.NAME) }
            }
        }
    }

    @DisplayName("Should ignore excluded sources")
    @CommonTest
    fun ignoreExcludedSources(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            withFailingSources()

            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.filter { exclude("**/fail-source.kt") }
                """.trimIndent()
            )

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    @DisplayName("Should fail on additional source set directories files style violation")
    @CommonTest
    fun additionalSourceSetsViolations(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            val alternativeDirectory = "src/main/shared"
            projectPath.withAlternativeFailingSources(alternativeDirectory)

            //language=Groovy
            buildGradle.appendText(
                """

                sourceSets {
                    findByName("main")?.java?.srcDirs(project.file("$alternativeDirectory"))
                }
                """.trimIndent()
            )

            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            }
        }
    }

    @DisplayName("Should always format again restored to pre-format state sources")
    @CommonTest
    fun repeatFormatForRestoredSources(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withFailingSources()
            val formatTaskName = KtLintFormatTask.buildTaskNameForSourceSet("main")

            build(FORMAT_PARENT_TASK_NAME) {
                assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            restoreFailingSources()

            build(FORMAT_PARENT_TASK_NAME) {
                assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(projectPath.resolve(FAIL_SOURCE_FILE)).exists()
            }

            build(CHECK_PARENT_TASK_NAME)
        }
    }

    @DisplayName("Format task should be UP-TO-DATE on 3rd run")
    @CommonTest
    fun formatUpToDate(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withFailingSources()
            val formatTaskName = KtLintFormatTask.buildTaskNameForSourceSet("main")

            build(FORMAT_PARENT_TASK_NAME) {
                assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
            build(FORMAT_PARENT_TASK_NAME) {
                assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
            build(FORMAT_PARENT_TASK_NAME) {
                assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            }
        }
    }

    @DisplayName("Format task should not create directories for empty SourceSets")
    @CommonTest
    fun formatNotCreateEmpty(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withFailingSources()

            build(FORMAT_PARENT_TASK_NAME) {
                assertThat(projectPath.resolve("src/main/java")).doesNotExist()
            }
        }
    }

    @DisplayName("Should apply KtLint version from extension")
    @CommonTest
    fun ktlintVersionFromExtension(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()

            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.version = "0.35.0"
                """.trimIndent()
            )

            build(":dependencies") {
                assertThat(output).contains(
                    "$KTLINT_CONFIGURATION_NAME - $KTLINT_CONFIGURATION_DESCRIPTION${System.lineSeparator()}" +
                        "\\--- com.pinterest:ktlint:0.35.0${System.lineSeparator()}"
                )
            }
        }
    }

    @DisplayName("Should check Kotlin script file in project folder")
    @CommonTest
    fun checkKotlinScript(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            withCleanKotlinScript()

            build(kotlinScriptCheckTaskName) {
                assertThat(task(":$kotlinScriptCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    @DisplayName("Should fail check for Kotlin script file in project folder with style violations")
    @CommonTest
    fun checkAndFailKotlinScript(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            withFailingKotlinScript()

            buildAndFail(kotlinScriptCheckTaskName) {
                assertThat(task(":$kotlinScriptCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            }
        }
    }

    @DisplayName("Should not check Kotlin script file in child project folder")
    @CommonTest
    fun ignoreKotlinScript(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            val additionalFolder = projectPath.resolve("scripts/").also { it.mkdirs() }
            additionalFolder.withFailingKotlinScript()

            build(kotlinScriptCheckTaskName) {
                assertThat(task(":$kotlinScriptCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SKIPPED)
            }
        }
    }

    @DisplayName("Should check kts files in configured child project folder")
    @CommonTest
    fun checkAdditionallyAddedKtsFiles(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            val additionalFolder = projectPath.resolve("scripts/")
            additionalFolder.withCleanKotlinScript()
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.kotlinScriptAdditionalPaths { include fileTree("scripts/") }
                """.trimIndent()
            )

            build(kotlinScriptCheckTaskName) {
                assertThat(task(":$kotlinScriptCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    @DisplayName("Should apply internal git filter to check task")
    @CommonTest
    fun gitFilterOnCheck(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            withFailingSources()

            build(
                ":$CHECK_PARENT_TASK_NAME",
                "-P$FILTER_INCLUDE_PROPERTY_NAME=src/main/kotlin/clean-source.kt"
            ) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    @DisplayName("Internal Git filter works with Windows")
    @CommonTest
    @EnabledOnOs(OS.WINDOWS)
    fun gitFilterOnCheckWindows(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()
            withFailingSources()

            build(
                ":$CHECK_PARENT_TASK_NAME",
                "-P$FILTER_INCLUDE_PROPERTY_NAME=src\\main\\kotlin\\clean-source.kt"
            ) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }
        }
    }

    @DisplayName("Git filter should respect already applied filters")
    @CommonTest
    fun gitFilterAlreadyAppliedFilters(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withFailingSources()
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.filter { exclude("**/fail-source.kt") }
                """.trimIndent()
            )

            build(
                ":$CHECK_PARENT_TASK_NAME",
                "-P$FILTER_INCLUDE_PROPERTY_NAME=src/main/kotlin/fail-source.kt"
            ) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SKIPPED)
            }
        }
    }

    @DisplayName("Git filter should ignore task if no files related to it")
    @CommonTest
    fun gitFilterIgnoreTask(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()

            build(
                ":$CHECK_PARENT_TASK_NAME",
                "-P$FILTER_INCLUDE_PROPERTY_NAME=src/main/kotlin/failing-sources.kt"
            ) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SKIPPED)
            }
        }
    }

    @DisplayName("Should enable experimental indentation rule")
    @CommonTest
    fun enableExperimentalIndentationRule(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            //language=Kotlin
            createSourceFile(
                "src/main/kotlin/C.kt",
                """
                    class C {

                        private val Any.className
                            get() = this.javaClass.name
                                .fn()

                        private fun String.escape() =
                            this.fn()
                    }
                """.trimIndent()
            )
            //language=Groovy
            buildGradle.appendText(
                """

                ktlint.enableExperimentalRules = true
                ktlint.version = "0.34.0"
                """.trimIndent()
            )

            buildAndFail(":$CHECK_PARENT_TASK_NAME", "-s") {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            }
        }
    }

    @DisplayName("Lint check should run incrementally")
    @CommonTest
    fun checkIsIncremental(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val initialSourceFile = "src/main/kotlin/initial.kt"
            createSourceFile(
                initialSourceFile,
                """
                val foo = "bar"

                """.trimIndent()
            )

            build(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            val additionalSourceFile = "src/main/kotlin/another-file.kt"
            createSourceFile(
                additionalSourceFile,
                """
                val bar = "foo"

                """.trimIndent()
            )

            build(CHECK_PARENT_TASK_NAME, "--info") {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                assertThat(output).contains("Executing incrementally")
            }
        }
    }

    @DisplayName("Should check files which path conatins whitespace")
    @CommonTest
    fun pathsWithWhitespace(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            createSourceFile(
                "src/main/kotlin/some path with whitespace/some file.kt",
                """
                class Test
                """.trimIndent()
            )

            buildAndFail(CHECK_PARENT_TASK_NAME) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.FAILED)
            }
        }
    }

    @DisplayName("Should do nothing when there are no eligible incremental updates")
    @CommonTest
    fun noIncrementalUpdates(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val passingContents =
                """
                val foo = "bar"

                """.trimIndent()

            val failingContents =
                """
                val foo="bar"

                """.trimIndent()

            val initialSourceFile = "src/main/kotlin/initial.kt"
            createSourceFile(initialSourceFile, passingContents)

            val additionalSourceFile = "src/main/kotlin/another-file.kt"
            createSourceFile(additionalSourceFile, passingContents)

            val testSourceFile = "src/test/kotlin/another-file.kt"
            createSourceFile(testSourceFile, failingContents)

            build(mainSourceSetCheckTaskName) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            // Removing a source file will cause the task to run, but the only incremental change will
            // be REMOVED, which does need to call ktlint
            removeSourceFile(initialSourceFile)
            build(mainSourceSetCheckTaskName) {
                assertThat(task(":$mainSourceSetCheckTaskName")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
            }
        }
    }

    @DisplayName("Should not leak KtLint into buildscript classpath")
    @CommonTest
    fun noLeakIntoBuildscriptClasspath(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()

            build("buildEnvironment") {
                assertThat(output).doesNotContain("com.pinterest.ktlint")
            }
        }
    }

    /**
     * See: https://github.com/JLLeitschuh/ktlint-gradle/issues/523#issuecomment-1022522032
     */
    @DisplayName("Should not leak KtLint as a variant into consuming projects")
    @CommonTest
    fun noLeakIntoConsumingProjects(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            val producerDir = projectPath.resolve("producer").also { it.mkdirs() }
            val consumerDir = projectPath.resolve("consumer").also { it.mkdirs() }
            settingsGradle.appendText(
                """

                include ":producer"
                include ":consumer"
                """.trimIndent()
            )

            //language=Groovy
            producerDir.buildFile().writeText(
                """
                plugins {
                    id "org.jlleitschuh.gradle.ktlint"
                }
                configurations.create('default')
                artifacts {
                    add('default', buildFile)
                }
                """.trimIndent()
            )

            //language=Groovy
            consumerDir.buildFile().writeText(
                """
                plugins {
                    id 'java'
                }
                dependencies {
                    implementation project(':producer')
                }
                """.trimIndent()
            )

            build(":consumer:dependencies") {
                assertThat(output).doesNotContain("com.pinterest:ktlint")
            }
        }
    }

    @DisplayName("Should print paths to the generated reports on code style violations")
    @CommonTest
    fun printReportsPaths(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withFailingSources()

            buildAndFail(CHECK_PARENT_TASK_NAME) {
                val s = File.separator
                assertThat(output).contains(
                    "build${s}reports${s}ktlint${s}ktlintMainSourceSetCheck${s}ktlintMainSourceSetCheck.txt"
                )
            }
        }
    }

    @DisplayName("Should force dependencies versions from KtLint configuration for ruleset configuration")
    @CommonTest
    fun forceDependenciesRuleSetConfiguration(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()

            //language=Groovy
            buildGradle.appendText(
                """

                dependencies {
                    $KTLINT_RULESET_CONFIGURATION_NAME "com.pinterest.ktlint:ktlint-core:0.34.2"
                }
                """.trimIndent()
            )

            build(":dependencies", "--configuration", KTLINT_RULESET_CONFIGURATION_NAME) {
                assertThat(output).contains("com.pinterest.ktlint:ktlint-core:0.34.2 -> 0.44.0")
            }
        }
    }

    @DisplayName("Should force dependencies versions from KtLint configuration for reporters configuration")
    @CommonTest
    fun forceDependenciesReportersConfiguration(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withCleanSources()

            //language=Groovy
            buildGradle.appendText(
                """

                dependencies {
                    $KTLINT_REPORTER_CONFIGURATION_NAME "com.pinterest.ktlint:ktlint-core:0.34.2"
                }
                """.trimIndent()
            )

            build(":dependencies", "--configuration", KTLINT_REPORTER_CONFIGURATION_NAME) {
                assertThat(output).contains("com.pinterest.ktlint:ktlint-core:0.34.2 -> 0.44.0")
            }
        }
    }

    @DisplayName("Format task should succeed on renamed file")
    @CommonTest
    fun formatShouldSucceedOnRenamedFile(gradleVersion: GradleVersion) {
        project(gradleVersion) {
            withFailingSources()

            val formatTaskName = KtLintFormatTask.buildTaskNameForSourceSet("main")
            build(FORMAT_PARENT_TASK_NAME) {
                assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            val sourceFile = projectPath.resolve(FAIL_SOURCE_FILE)
            sourceFile.writeText(
                """
                val  foo    =    "bar"
            """
            )
            val destinationFile = projectPath.resolve("src/main/kotlin/renamed-file.kt")
            sourceFile.renameTo(destinationFile)

            build(FORMAT_PARENT_TASK_NAME) {
                assertThat(task(":$formatTaskName")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
            }

            build(CHECK_PARENT_TASK_NAME)
        }
    }
}
