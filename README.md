# Ktlint Gradle

**Provides a convenient wrapper plugin over the [ktlint](https://github.com/pinterest/ktlint) project.**

Latest plugin version: [9.1.0](/CHANGELOG.md#910---2019-11-01)

[![Join the chat at https://kotlinlang.slack.com](https://img.shields.io/badge/slack-@kotlinlang/ktlint-yellow.svg?logo=slack)](https://kotlinlang.slack.com/messages/CKS3XG0LS)
[![Build Status](https://travis-ci.org/JLLeitschuh/ktlint-gradle.svg?branch=master)](https://travis-ci.org/JLLeitschuh/ktlint-gradle)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/org/jlleitschuh/gradle/ktlint/org.jlleitschuh.gradle.ktlint.gradle.plugin/maven-metadata.xml.svg?colorB=007ec6&label=gradlePluginPortal)](https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint)

This plugin creates convenient tasks in your Gradle project
that run [ktlint](https://github.com/pinterest/ktlint) checks or do code
auto format.

Plugin can be applied to any project, but only activates if that project has the kotlin plugin applied.
The assumption being that you would not want to lint code you weren't compiling.

## Table of content
- [Supported Kotlin plugins](#supported-kotlin-plugins)
- [How to use](#how-to-use)
  - [Minimal support versions](#minimal_supported_versions)
  - [Ktlint plugin](#ktlint-plugin)
    - [Simple setup](#simple-setup)
    - [Using new plugin API](#using-new-plugin-api)
    - [How to apply to all subprojects](#applying-to-subprojects)
  - [Intellij IDEA plugin](#intellij-idea-only-plugin)
    - [Simple setup](#idea-plugin-simple-setup)
    - [Using new plugin API](#idea-plugin-setup-using-new-plugin-api)
  - [Plugin configuration](#configuration)
    - [Customer reporters](#custom-reporters)
  - [Samples](#samples)
- [Task details](#task-added)
  - [Main tasks](#main-tasks)
  - [Additional tasks](#additional-helper-tasks)
- [FAQ](#faq)
- [Developers](#developers)
  - [Importing the project](#importing)
  - [Building the project](#building)
- [Links](#links)

## Supported Kotlin plugins

This plugin supports following kotlin plugins:
- "kotlin"
- "kotlin-android"
- "kotlin2js"
- "kotlin-platform-common"
- "kotlin-platform-js"
- "kotlin-platform-jvm"
- "kotlin-multiplatform"
- project kotlin script files
- "org.jetbrains.kotlin.js"

If you know any new Kotlin plugin that are not in this list - please,
open a [new issue](https://github.com/JLLeitschuh/ktlint-gradle/issues/new).

## How to use

### Minimal supported versions

This plugin was written using the new API available for gradle script kotlin builds.
This API is available in new versions of gradle.

Minimal supported [Gradle](www.gradle.org) version: `5.4.1`

Minimal supported [ktlint](https://github.com/pinterest/ktlint) version: `0.22.0`

### Ktlint plugin

#### Simple setup

Build script snippet for use in all Gradle versions:
```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "org.jlleitschuh.gradle:ktlint-gradle:<current_version>"
  }
}

apply plugin: "org.jlleitschuh.gradle.ktlint"
```


#### Using new plugin API

Build script snippet for new, incubating, plugin mechanism introduced in Gradle 2.1:
```groovy
plugins {
  id "org.jlleitschuh.gradle.ktlint" version "<current_version>"
}
```


#### Applying to subprojects

Optionally apply plugin to all project modules:
```groovy
subprojects {
    apply plugin: "org.jlleitschuh.gradle.ktlint" // Version should be inherited from parent
    
    // Optionally configure plugin
    ktlint {
       debug = true
    }
}
```

### IntelliJ Idea Only Plugin

**Note:** This plugin is automatically applied by the main `ktlint` plugin.

This plugin just adds [special tasks](#additional-helper-tasks) that can generate IntelliJ IDEA codestyle
rules using ktlint.

#### Idea plugin simple setup

For all gradle versions:

Use the same `buildscript` logic as [above](#simple-setup), but with this instead of the above suggested `apply` line.

```groovy
apply plugin: "org.jlleitschuh.gradle.ktlint-idea"
```

#### Idea plugin setup using new plugin API

Build script snippet for new, incubating, plugin mechanism introduced in Gradle 2.1:
```groovy
plugins {
  id "org.jlleitschuh.gradle.ktlint-idea" version "<current_version>"
}
```

### Configuration
The following configuration block is _optional_.

If you don't configure this the defaults defined
in the [KtlintExtension](plugin/src/main/kotlin/org/jlleitschuh/gradle/ktlint/KtlintExtension.kt)
object will be used.

The version of ktlint used by default _may change_ between patch versions of this plugin.
If you don't want to inherit these changes then make sure you lock your version here.

```groovy
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

ktlint {
    version = "0.22.0"
    debug = true
    verbose = true
    android = false
    outputToConsole = true
    outputColorName = "RED"
    ignoreFailures = true
    enableExperimentalRules = true
    additionalEditorconfigFile = file("/some/additional/.editorconfig")
    disabledRules = ["final-newline"]
    reporters {
        reporter "plain"
        reporter "checkstyle"
        
        customReporters {
            "csv" {
                fileExtension = "csv"
                dependency = project(":project-reporters:csv-reporter")
            }
            "yaml" {
                fileExtension = "yml"
                dependency = "com.example:ktlint-yaml-reporter:1.0.0"
            }
        }
    }
    kotlinScriptAdditionalPaths {
        include fileTree("scripts/")
    }
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

dependencies {
    ktlintRuleset "com.github.username:rulseset:master-SNAPSHOT"
    ktlintRuleset files("/path/to/custom/rulseset.jar")
    ktlintRuleset project(":chore:project-ruleset") 
}
```

or in kotlin script:
```kotlin
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

ktlint {
    version.set("0.22.0")
    debug.set(true)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(true)
    enableExperimentalRules.set(true)
    additionalEditorconfigFile.set(file("/some/additional/.editorconfig"))
    disabledRules.set(setOf("final-newline"))
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
        
        customReporters {
            register("csv") {
                fileExtension = "csv"
                dependency = project(":project-reporters:csv-reporter")
            }
            register("yaml") {
                fileExtension = "yml"
                dependency = "com.example:ktlint-yaml-reporter:1.0.0"
            }
        }
    }
    kotlinScriptAdditionalPaths {
        include(fileTree("scripts/"))
    }
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

dependencies {
    ktlintRuleset("com.github.username:rulseset:master-SNAPSHOT")
    ktlintRuleset(files("/path/to/custom/rulseset.jar"))
    ktlintRuleset(project(":chore:project-ruleset")) 
}
```

#### Custom reporters

**Note**: If Ktlint custom reporter creates report output file internally, for example:
```kotlin
class CsvReporter(
    private val out: PrintStream
) : Reporter {
    override fun onLintError(file: String, err: LintError, corrected: Boolean) {
        val line = "$file;${err.line};${err.col};${err.ruleId};${err.detail};$corrected"
        out.println(line)
        File("some_other_file.txt").write(line) // <-- Here!!!
    }
}
```
"some_other_file.txt" won't be captured as task output. This may lead to the problem,
that task will always be not "UP_TO_DATE" and caching will not work.

### Samples

This repository provides following examples how to setup this plugin:
- [android-app](/samples/android-app) - applies plugin to android application project
- [kotlin-gradle](/samples/kotlin-gradle) - applies plugin to plain Kotlin project that uses groovy in `build.gradle` files
- [kotlin-js](/samples/kotlin-js) - applies plugin to kotlin js project
- [kotlin-ks](/samples/kotlin-ks) - applies plugin to plain Kotlin project that uses Kotlin DSL in `build.gradle.kts` files
- [kotlin-multiplatform-common](/samples/kotlin-multiplatform-common) - applies plugin to Kotlin common multiplatform module
- [kotlin-multiplatform-js](/samples/kotlin-multiplatform-js) - applies plugin to Kotlin Javascript multiplatform module
- [kotlin-multiplatform-jvm](/samples/kotlin-multiplatform-jvm) - applies plugin to Kotlin JVM multiplatform module
- [kotlin-rulesets-using](/samples/kotlin-rulesets-using) - adds custom [example](/samples/kotlin-ruleset-creating) ruleset
- [kotlin-reporter-using](/samples/kotlin-reporter-using) - adds custom [example](/samples/kotlin-reporter-creating) reporter 

## Tasks Added

### Main tasks

This plugin adds two maintasks to every source set: `ktlint[source set name]SourceSetCheck` and `ktlint[source set name]SourceSetFormat`.
Additionally, a simple `ktlintCheck` task has also been added that checks all of the source sets for that project.
Similarly, a `ktlintFormat` task has been added that formats all of the source sets.

Android projects, additionally, will have meta tasks for Android variants, that will process all source sets in variant.
For example, if app has `foo` flavor, following meta tasks will be added:
`ktlintFooDebugCheck`, `ktlintFooReleaseCheck`, `ktlintFooDebugFormat`, `ktlintFooReleaseFormat`.

Additionally plugin adds two task for project kotlin script files: `ktlintKotlinScriptCheck` and `ktlintKotlinScriptFormat`.

### Additional helper tasks

Following additional  tasks are added:
- `ktlintApplyToIdea` - The task generates IntelliJ IDEA (or Android Studio) Kotlin
                        style files in the project `.idea/` folder. **Note** that this tasks will overwrite the existing style file.
- `ktlintApplyToIdeaGlobally` - The task generates IntelliJ IDEA (or Android Studio) Kotlin
                                style files in the user home IDEA
                                (or Android Studio) settings folder. **Note** that this task will overwrite the existing style file.
- `addKtlintCheckGitPreCommitHook` - adds [Git](https://www.git-scm.com/) `pre-commit` hook, 
that runs ktlint check over staged files.
- `addKtlintFormatGitPreCommitHook` - adds [Git](https://www.git-scm.com/) `pre-commit` hook, 
that runs ktlint format over staged files and adds fixed files back to commit.

All this additional tasks are always added **only** to the root project.

## FAQ

- Is it possible to not stop tasks execution if some of subprojects tasks failed?

Yes. Just use gradle `--continue` option:
```shell
$ ./gradlew --continue ktlintCheck
```

- Can I mix old plugin and new plugin API setup in my project
(see [simple-setup](#simple-setup) and [using new plugin API setup](#using-new-plugin-api))?

No. This approaches are not equivalent how they work. The problem that
plugin may not find some of kotlin plugins if both approaches are used
in the project configuration. Especially it is related to Android plugin.

- Does plugin check changed files incrementally?

Yes. On first run plugin will check all files in the module, on
subsequent runs it will check only added/modified files.

## Developers

### Importing

Import the [settings.gradle.kts](settings.gradle.kts) file into your IDE.

To enable the Android sample either define the `ANDROID_HOME` environmental variable or
add a `local.properties` file to the project root folder with the following content:
```properties
sdk.dir=<android-sdk-location>
```

### Building

Building the plugin: `./plugin/gradlew build`

On how to run the current plugin snapshot check on sample projects: `./gradlew ktlintCheck`

### Running tests from [IDEA IDE](https://www.jetbrains.com/idea/)

To run tests in [IDEA IDE](https://www.jetbrains.com/idea/), 
firstly you need to run following gradle task (or after any dependency change):

```bash
$ ./plugin/gradlew pluginUnderTestMetadata
```

Optionally you can add this step test run configuration.

## Links

[Ktlint Gradle Plugin on the Gradle Plugin Registry](https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint)
