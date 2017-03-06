# Ktlint Gradle

Provides a convenient wrapper plugin over the [ktlint](https://github.com/shyiko/ktlint) project.

This plugin can be applied to any project but only activates if that project has the kotlin plugin applied.
The assumption being that you would not want to lint code you weren't compiling.

## Warning

This uses/publishes with a beta version of gradle. I plan to fix this as soon as the newest version of gradle script kotlin
is released with a stable version of kotlin.


## How to use

**TOOD**

Currently this plugin only supports publishing to your local M2 repository. I plan to publish this soon.


Gradle script kotlin usage example:
```kotlin
buildscript {
    repositories {
        // TODO: Publish to a public maven repo
        mavenLocal()
    }
}

apply {
    plugin("org.jlleitschuh.gradle.ktlint")
}
```

## Tasks Added

This plugin adds two tasks to every source set: `ktlint[source set name]` and `ktlint[source set name]Format`.
Additionally, a simple `ktlint` task has also been added that checks all of the source sets for that project.
Similarly, a `ktlintFormat` task has been added that formats all of the source sets.


## Developers

#### Building

`./gradlew build`

### Future Development

Add support for linting `*.kts` for gradle script kotlin builds.
