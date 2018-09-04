# Ktlint Gradle

[![Join the chat at https://gitter.im/ktlint-gradle/Lobby](https://badges.gitter.im/ktlint-gradle/Lobby.svg)](https://gitter.im/ktlint-gradle/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/JLLeitschuh/ktlint-gradle.svg?branch=master)](https://travis-ci.org/JLLeitschuh/ktlint-gradle)

Provides a convenient wrapper plugin over the [ktlint](https://github.com/shyiko/ktlint) project.

This plugin can be applied to any project but only activates if that project has the kotlin plugin applied.
The assumption being that you would not want to lint code you weren't compiling.

## Warning

This plugin was written using the new API available for gradle script kotlin builds.
This API is available in new versions of gradle.

Minimal supported Gradle version: `4.3`

Minimal supported [ktlint](https://github.com/shyiko/ktlint) version: `0.10.0`

## How to use

### Simple

Build script snippet for use in all Gradle versions:
```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.org.jlleitschuh.gradle:ktlint-gradle:5.0.0"
  }
}

apply plugin: "org.jlleitschuh.gradle.ktlint"
```

Build script snippet for new, incubating, plugin mechanism introduced in Gradle 2.1:
```groovy
plugins {
  id "org.jlleitschuh.gradle.ktlint" version "5.0.0"
}
```

Optionally apply plugin to all project modules:
```groovy
subprojects {
    apply plugin: "org.jlleitschuh.gradle.ktlint" // Version should be inherited from parent
}
```

### IntelliJ Idea Only Plugin

(This plugin is automatically applied by the `ktlint` plugin.)

For all gradle versions:

Use the same `buildscript` logic as above but with this instead of the above suggested `apply` line.

```groovy
apply plugin: "org.jlleitschuh.gradle.ktlint-idea"
```

Build script snippet for new, incubating, plugin mechanism introduced in Gradle 2.1:
```groovy
plugins {
  id "org.jlleitschuh.gradle.ktlint-idea" version "5.0.0"
}
```

## Configuration
The following configuration block is optional.

If you don't configure this the defaults defined in the [KtlintExtension](plugin/src/main/kotlin/org/jlleitschuh/gradle/ktlint/KtlintExtension.kt) object will be used.
The version of Ktlint used by default may change between patch versions of this plugin. If you don't want to inherit these changes then make sure you lock your version here.
```groovy
ktlint {
    version = ""
    debug = true
    verbose = true
    android = false
    outputToConsole = true
    reporters = ["PLAIN", "CHECKSTYLE"]
    ignoreFailures = true
    ruleSets = [
        "/path/to/custom/rulseset.jar",
        "com.github.username:rulseset:master-SNAPSHOT"
    ]
}
```

## Samples

Check [samples](samples/) folder that provides examples how-to apply plugin.

## Tasks Added

This plugin adds two tasks to every source set: `ktlint[source set name]Check` and `ktlint[source set name]Format`.
Additionally, a simple `ktlintCheck` task has also been added that checks all of the source sets for that project.
Similarly, a `ktlintFormat` task has been added that formats all of the source sets.

If project has subprojects - plugin also adds two meta tasks `ktlintCheck` and `ktlintFormat` to the root project that
triggers related tasks in subprojects.

### Apply to IDEA
Two another tasks added:
- `ktlintApplyToIdea` - Task generates IntelliJ IDEA (or Android Studio) Kotlin
                        style files in project `.idea/` folder.
- `ktlintApplyToIdeaGlobally` - Task generates IntelliJ IDEA (or Android Studio) Kotlin
                                style files in user home IDEA
                                (or Android Studio) settings folder.

They are always added only to the root project.

**Note** that this tasks will overwrite the existing style file.

## Developers

### IDE Support

Import the [settings.gradle.kts](settings.gradle.kts) file into your IDE.

To enable Android sample either define `ANDROID_HOME` environmental variable or
add `local.properties` file to project root folder with following content:
```properties
sdk.dir=<android-sdk-location>
```

#### Building

Building the plugin: `./plugin/gradlew build`

Running current plugin snapshot check on sample projects: `./gradlew ktlintCheck`

## Links

[Ktlint Gradle Plugin on the Gradle Plugin Registry](https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint)
