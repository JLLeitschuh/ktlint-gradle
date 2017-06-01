# Ktlint Gradle

[![Join the chat at https://gitter.im/ktlint-gradle/Lobby](https://badges.gitter.im/ktlint-gradle/Lobby.svg)](https://gitter.im/ktlint-gradle/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/JLLeitschuh/ktlint-gradle.svg?branch=master)](https://travis-ci.org/JLLeitschuh/ktlint-gradle)

Provides a convenient wrapper plugin over the [ktlint](https://github.com/shyiko/ktlint) project.

This plugin can be applied to any project but only activates if that project has the kotlin plugin applied.
The assumption being that you would not want to lint code you weren't compiling.

## Warning

This plugin was written using the new API available for gradle script kotlin builds.
This API is available in new versions of gradle.

This plugin has only been tested with gradle `3.5` and should work with versions `3.5+`.

If you find this plugin works with older versions feel free to update this readme to reflect this.


## How to use

Build script snippet for use in all Gradle versions:
```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.org.jlleitschuh.gradle:ktlint-gradle:2.0.1"
  }
}

apply plugin: "org.jlleitschuh.gradle.ktlint"

```

Build script snippet for new, incubating, plugin mechanism introduced in Gradle 2.1:
```groovy
plugins {
  id "org.jlleitschuh.gradle.ktlint" version "2.0.1"
}
```

## Configuration
The following configuration block is optional.

If you don't configure this the defaults defined in the [KtlintExtension](src/main/kotlin/org/jlleitschuh/gradle/ktlint/KtlintExtension.kt) object will be used.
The version of Ktlint used by default may change between patch versions of this plugin. If you don't want to inherit these changes then make sure you lock your version here.
```groovy
ktlint {
    version = ""
    debug = true
    verbose = true
}
```

## Tasks Added

This plugin adds two tasks to every source set: `ktlint[source set name]Check` and `ktlint[source set name]Format`.
Additionally, a simple `ktlintCheck` task has also been added that checks all of the source sets for that project.
Similarly, a `ktlintFormat` task has been added that formats all of the source sets.


## Developers

#### Building

`./gradlew build`

### Future Development

Add support for linting `*.kts` for gradle script kotlin builds.

## Links

[Ktlint Gradle Plugin on the Gradle Plugin Registry](https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint)
