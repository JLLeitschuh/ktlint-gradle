# Ktlint Gradle

**Provides a convenient wrapper plugin over the [ktlint](https://github.com/shyiko/ktlint) project.**

Latest plugin version: [6.0.0](/CHANGELOG.md#600---2018-9-20)

[![Join the chat at https://gitter.im/ktlint-gradle/Lobby](https://badges.gitter.im/ktlint-gradle/Lobby.svg)](https://gitter.im/ktlint-gradle/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/JLLeitschuh/ktlint-gradle.svg?branch=master)](https://travis-ci.org/JLLeitschuh/ktlint-gradle)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)

This plugin creates convenient tasks in your Gradle project
that run [ktlint](https://github.com/shyiko/ktlint) checks or do code
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
- "konan"
- "org.jetbrains.kotlin.native"

If you know any new Kotlin plugin that are not in this list - please,
open a [new issue](https://github.com/JLLeitschuh/ktlint-gradle/issues/new).

## How to use

### Minimal supported versions

This plugin was written using the new API available for gradle script kotlin builds.
This API is available in new versions of gradle.

Minimal supported [Gradle](www.gradle.org) version: `4.3`

Minimal supported [ktlint](https://github.com/shyiko/ktlint) version: `0.10.0`

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
    classpath "gradle.plugin.org.jlleitschuh.gradle:ktlint-gradle:<current_version>"
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
    reporters = [ReporterType.PLAIN, ReporterType.CHECKSTYLE]
    ignoreFailures = true
    ruleSets = [
        "/path/to/custom/rulseset.jar",
        "com.github.username:rulseset:master-SNAPSHOT"
    ]
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
    reporters.set(setOf(ReporterType.PLAIN, ReporterType.CHECKSTYLE))
    ignoreFailures.set(true)
    ruleSets.set(listOf(
        "/path/to/custom/rulseset.jar",
        "com.github.username:rulseset:master-SNAPSHOT"
    ))
}
```

### Samples

This repository provides following examples how to setup this plugin:
- [android-app](/samples/android-app) - applies plugin to android application project
- [kotlin-gradle](/samples/kotlin-gradle) - applies plugin to plain Kotlin project that uses groovy in `build.gradle` files
- [kotlin-js](/samples/kotlin-js) - applies plugin to kotlin js project
- [kotlin-ks](/samples/kotlin-ks) - applies plugin to plain Kotlin project that uses Kotlin DSL in `build.gradle.kts` files
- [kotlin-multiplatform-common](/samples/kotlin-multiplatform-common) - applies plugin to Kotlin common multiplatform module
- [kotlin-multiplatform-js](/samples/kotlin-multiplatform-js) - applies plugin to Kotlin Javascript multiplatform module
- [kotlin-multiplatform-jvm](/samples/kotlin-multiplatform-jvm) - applies plugin to Kotlin JVM multiplatform module
- [kotlin-native](/samples/kotlin-native) - applies plugin to Kotlin native project
- [kotlin-rulesets-using](/samples/kotlin-rulesets-using) - adds custom [example](/samples/kotlin-ruleset-creating) ruleset

## Tasks Added

### Main tasks

This plugin adds two tasks to every source set: `ktlint[source set name]Check` and `ktlint[source set name]Format`.
Additionally, a simple `ktlintCheck` task has also been added that checks all of the source sets for that project.
Similarly, a `ktlintFormat` task has been added that formats all of the source sets.

If the project has subprojects then the plugin also adds two meta tasks `ktlintCheck` and `ktlintFormat` to the root project that
triggers the related tasks in the subprojects.

### Additional helper tasks

Two another tasks are added:
- `ktlintApplyToIdea` - The task generates IntelliJ IDEA (or Android Studio) Kotlin
                        style files in the project `.idea/` folder.
- `ktlintApplyToIdeaGlobally` - The task generates IntelliJ IDEA (or Android Studio) Kotlin
                                style files in the user home IDEA
                                (or Android Studio) settings folder.

They are always added **only** to the root project.

**Note** that this tasks will overwrite the existing style file.

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

## Links

[Ktlint Gradle Plugin on the Gradle Plugin Registry](https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint)
