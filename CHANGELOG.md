# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/)
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]
### Fixed
  - Fix install hook action when git `hooks` folder doesn't exist [issue: #557](https://github.com/JLLeitschuh/ktlint-gradle/issues/557), [#563](https://github.com/JLLeitschuh/ktlint-gradle/pull/563)
  - Fix some resolution issues when a project using the plugin in some specific setups is depended upon by another project [issue: #523](https://github.com/JLLeitschuh/ktlint-gradle/issues/523), [#571](https://github.com/JLLeitschuh/ktlint-gradle/pull/571)

## [10.2.1] - 2021.12.27
### Fixed
  - Deleted file causes file not found exception ([issue: #539](https://github.com/JLLeitschuh/ktlint-gradle/issues/539), [#548](https://github.com/JLLeitschuh/ktlint-gradle/pull/548))
  - Use Gradle command exit code as hook exit code to ensure un-staged changes are always re-applied to the working directory [#551](https://github.com/JLLeitschuh/ktlint-gradle/pull/551)

## [10.2.0] - 2021.09.08
### Added
  - sarif reporter to provided reporters ([#516](https://github.com/JLLeitschuh/ktlint-gradle/pull/516))

### Changed
  - Update Gradle to `7.1.1` version
  - Update Shadow plugin to `7.0.0` version
  - Update Kotlin to `1.5.21` version
  - Set default KtLint version to `0.42.1`
  - Rethink format task approach ([issue: #306](https://github.com/JLLeitschuh/ktlint-gradle/issues/306))

### Fixed
  - Pre-commit hook causing conflicts ([issue: #443](https://github.com/JLLeitschuh/ktlint-gradle/issues/443)) ([#502](https://github.com/JLLeitschuh/ktlint-gradle/pull/502))
  - `ktlintFormat` create empty directories in `src/` dir ([issue: #423](https://github.com/JLLeitschuh/ktlint-gradle/issues/423))
  - Add Git hook task breaks configuration cache ([issue: #505](https://github.com/JLLeitschuh/ktlint-gradle/issues/505))
  - Plugin failed to apply on eager tasks creation ([issue: #495](https://github.com/JLLeitschuh/ktlint-gradle/issues/495))

## [10.1.0] - 2021.06.02
### Added
  - Baseline support ([#414](https://github.com/JLLeitschuh/ktlint-gradle/issues/414))

    Limitations:
    - Format tasks ignore baseline
    - One baseline file per-Gradle project (module)

### Changed
  - Updated Gradle to `6.8.3` version
  - Updated default KtLint version to `0.41.0`
### Fixed
  - Plugin fails to apply on non-Kotlin projects ([#443](https://github.com/JLLeitschuh/ktlint-gradle/issues/443))
  - Pre-commit hook adds entire file to commit when only part of the file was indexed ([#470](https://github.com/JLLeitschuh/ktlint-gradle/pull/470))
  - Pre-commit hook doesn't format files that have been renamed ([#471](https://github.com/JLLeitschuh/ktlint-gradle/pull/471))
  - Reset KtLint internal caches on any `.editorconfig` files changes ([#456](https://github.com/JLLeitschuh/ktlint-gradle/issues/456))
  - On KtLint parse error print path to file ([#476](https://github.com/JLLeitschuh/ktlint-gradle/issues/476))
  - Add workaround for format tasks showing deprecation messages in Gradle 7.0 ([#480](https://github.com/JLLeitschuh/ktlint-gradle/pull/480))

## [10.0.0] - 2021.02.09
### Changed
  - Updated Kotlin to `1.4.30` version.

    **Breaking** - removed support for following deprecated Kotlin plugins:
    1. "kotlin2js"
    2. "kotlin-platform-*"
  - Updated Android Gradle Plugin to `4.1.0` version.

    **Breaking** - removed build variants meta tasks. Minimum supported AGP version is `4.0.0`.
  - Updated shadow plugin to `6.1.0` version.
  - Set default ktlint version to `0.40.0`
  - Updated Gradle to `6.8.1` version
  - Set minimal supported Gradle version to `6.0`
  - Set minimal supported KtLint version to `0.34.0`
  - Use KtLint directly instead of invoking it via CLI [#424](https://github.com/JLLeitschuh/ktlint-gradle/pull/424)

    **Breaking**:
     1. Tasks classes were completely changed and new one were introduced. Configuration should stay the same,
     so, if you don't configure tasks directly, update should be done without issues.
     2. To configure reports output directory, use `GenerateReportsTask#reportsOutputDirectory` property.
     3. Errors in Gradle console does not use colors. If you still need it, please open a new issue.
     4. Linting is running in workers with process isolation.
     To configure maximum heap size, use `BaseKtLintCheckTask#workerMaxHeapSize` property.
     5. "ktlintRuleset" and "ktlintReporter" configurations dependencies versions are constraint by main "ktlint" configuration dependencies versions.

### Fixed
  - Gradle deprecations [#395](https://github.com/JLLeitschuh/ktlint-gradle/issues/395)
  - Fail task on KtLint crash [#229](https://github.com/JLLeitschuh/ktlint-gradle/issues/229)

## [9.4.1] - 2020.10.05
### Fixed
  - Plugin now correctly validates files on Windows OS [#399](https://github.com/JLLeitschuh/ktlint-gradle/issues/399)

## [9.4.0] - 2020.09.06
### Changed
  - Updated Gradle to `6.6.1` version
  - Each task will output reports into subdirectory inside `build/reports/ktlint` directory to fix non-working caching [#379](https://github.com/JLLeitschuh/ktlint-gradle/issues/379)
  - Set default ktlint version to `0.38.1`
### Fixed
  - KtLint was not checking files that contains whitespace in path or name [#362](https://github.com/JLLeitschuh/ktlint-gradle/issues/362)
  - Skip check if incremental changes contains only removed files [#385](https://github.com/JLLeitschuh/ktlint-gradle/issues/385)

## [9.3.0] - 2020.07.17
### Added
  - Allow to specify reporters output dir [#321](https://github.com/JLLeitschuh/ktlint-gradle/issues/321)
### Changed
  - Check pre-commit hook will not add partially committed files to git commit [#330](https://github.com/JLLeitschuh/ktlint-gradle/issues/330)
  - Update Gradle to `6.5.1` version
  - Update Android Gradle plugin to `3.6.3` version
  - Set default ktlint version to `0.37.2`
  - Executing ktlint uses now uses the [Gradle worker API](https://guides.gradle.org/using-the-worker-api/) when supported.
  - `ktlintFormat` and `ktlintCheck` tasks now support the [configuration cache](https://docs.gradle.org/nightly/userguide/configuration_cache.html) [#364](https://github.com/JLLeitschuh/ktlint-gradle/issues/364)
  -  Make pre-commit hook working with Windows path separator [#359] (https://github.com/JLLeitschuh/ktlint-gradle/pull/359)

## [9.2.1] - 2020-02-12
### Fixed
  - Git hook fails to check multiple files [#336](https://github.com/JLLeitschuh/ktlint-gradle/issues/336)

## [9.2.0] - 2020-02-10
### Added
  - Html reporter to provided reporters [#312](https://github.com/JLLeitschuh/ktlint-gradle/issues/312)
  - Plugin will search for project `.git` folder
    relative to gradle root project to install git hook [#284](https://github.com/JLLeitschuh/ktlint-gradle/issues/284)
### Changed
  - Update Gradle to `6.0.1` version
  - Update Kotlin to `1.3.60` version
  - Set default ktlint version to `0.36.0`
  - Shadow plugin dependencies into plugin jar
### Fixed
  - Fix `ktlintApplyToIdea` task fails when `android = true` is set [#311](https://github.com/JLLeitschuh/ktlint-gradle/issues/311)

## [9.1.1] - 2019-11-12
### Fixed
  - Running format task may delete source files (#302): disable incremental
  support for format tasks. Lint tasks are still incremental.

## [9.1.0] - 2019-11-01
### Added
  - Support for outputColorName property (#297)
  - Support for incremental checks (#231)
### Changed
  - Set default ktlint version to `0.35.0`
### Fixed
  - Ktlint configuration could be tried to configured after been resolved (#283)

## [9.0.0] - 2019-09-30
### Added
  - Breaking: Add support for 3rd party reporters:
    - new reporters configuration DSL (#125)
    - tasks `reportOutputFiles` property was replaced with `allReportsOutputFiles`
  - `disabledRules` extension property to disable rules usage by id (#267)
### Changed
  - Update Gradle to `5.6.2` version
  - Update Kotlin to `1.3.50` version
  - Set default ktlint version to `0.34.2`
  - Update Android Gradle plugin to `3.5.0` version
  - Minimal supported Gradle version updated to `5.4.1`
### Removed
  - Support for "konan" plugin
  - Support for "kotlin-native-gradle-plugin" plugin
  - Deprecated `ruleset` extension property, please use `ruleset` configuration instead
### Fixed
  - Task failing when command line arguments limit was reached (#233)

## [8.2.0] - 2019-07-18
### Added
  - Support for new JS plugin: "org.jetbrains.kotlin.js" (#252)
  - Support for android "com.android.dynamic-feature" plugin (#260)
### Changed
  - Update Kotlin to `1.3.41` version
### Fixed
  - Usage of bashisms in git hook script (#251)

## [8.1.0] - 2019-06-16
### Added
  - `additionalEditorconfigFile` property to plugin extension (#210)
### Changed
  - Default ktlint version to `0.33.0`.

## [8.0.0] - 2019-05-06
### Added
  - `ktlintRuleset` configuration to provide 3rd party ktlint rules (#71)
### Changed
  - Update Kotlin to `1.3.30` version
  - Deprecated providing 3rd party ktlint rules via extension (#71)
  - Breaking: Change tasks inheritance - now `KtlintFormat` and `KtlintCheck` extend `BaseKtlintCheckTask` (#225)
  - Update Android Gradle plugin to `3.4.0` version
### Fixed
  - Proper lazy adding ktlint dependency (#219)
  - Gradle 5.x deprecation messages (#208)

## [7.4.0] - 2019-04-23
### Added
 - Support different ktlint group/package name after pinterest ownership of ktlint project (#228)
### Changed
 - Default ktlint version to `0.32.0`.

## [7.3.0] - 2019-04-10
### Added
  - Git pre-commit hook (#101):

    Current implementation does not support `buildSrc` or composite builds.
  - Flag to enable experimental rules (#215)

## [7.2.1] - 2019-03-14
### Changed
  - Default ktlint version is set to `0.31.0`
### Fixed
  - Failed task verification on Gradle `5.2.1` (#217)

## [7.2.0] - 2019-03-13
### Added
  - Tasks to check and format kotlin script files (#98)
### Changed
  - Update Kotlin to `1.3.21` version
  - Update Android gradle plugin to `3.3.0` version
### Fixed
  - `.editorconfig` file in project root dir is not considered as tasks input (#209)

## [7.1.0] - 2019-02-05
### Added

  - Warning about using vulnerable ktlint version
### Changed
  - Default ktlint version is set to `0.30.0`
### Fixed
  - Used ktlint version is always default one (#198)
  - Gradle `5.2` fails the build in pure kotlin project (#201)

## [7.0.0] - 2019-01-31
### Added
  - Meta tasks to run check or format on all sources in android variant. (#170)
  Example: In an Android project with `foo` flavor,
  `ktlintFooDebugSourceSetCheck` task will check the `foo` sourceSet (not main).
  `ktlintFooDebugCheck` meta task will check all the sourceSets for `fooDebug` build variant.
  - Plugin tasks configuration avoidance
### Changed
  - Update Kotlin to `1.3.10` version
  - Breaking: check/format tasks for specific source sets
  and according reports outputs now include `SourceSet` in their name(#170)
  - Breaking: minimal supported Gradle version is `4.10`
  - Breaking: minimal supported ktlint version is `0.22.0`
### Removed
  - Adding explicit meta check and format tasks to the root project
### Fixed
  - Format task may produce up-to-date state if sources was restored to pre-format state (#194)

## [6.3.1] - 2018-11-27
### Fixed
  - Updated SemVer dependency to `1.1.1` version (#162)

## [6.3.0] - 2018-11-06
### Added
  - Added support for new kotlin multiplatform plugin (#144)
### Changed
  - Update Kotlin to `1.3.0` version
  - Sync native plugins version with kotlin main release version
### Fixed
  -  Failure on Windows due to long argument line (#156)

## [6.2.1] - 2018-10-30
### Fixed
  - Fixed additional android source dirs for SourceSet are not checked (#153)

## [6.2.0] - 2018-10-18
### Added
  - Allow to exclude sources from check (#97)
### Changed
  - Update Android gradle plugin version to `3.2.0`
  - Check and format tasks now extend `SourceTask` (#85)

## [6.1.0] - 2018-10-5
### Added
  - Console colored output (#50)
### Changed
  - Update Kotlin to `1.2.71` version
  - Update Gradle to `4.10.2` version
  - Update default KtLint version to `0.29.0`
  - Hide specific source sets tasks
### Fixed
  - Fixed plugin fails to configure android project with flavors (#131)

## [6.0.0] - 2018-9-20
### Added
  - Separate KtlintFormatTask task (#111)
### Changed
  - `ktlintApplyToIdea` task is always added, though it will fail on
  ktlint versions less then `0.22.0`
  - Plugin extension now uses Gradle properties for configuration
  - `ktlint.reporters` extension property has to use imported `ReporterType`
  in groovy Gradle build scripts.
  - reporters output file name changed to be the same as task name. For example for `PLAIN`
  it will be `ktlintMainCheck.txt`.
  - format tasks now are also generate reports. For example: `ktlintMainFormat.txt`.
### Removed
  - Usages of `afterEvaluate {}` in plugin and sample projects (#122)

## [5.1.0] - 2018-9-5
### Added
  - Support for new kotlin native experimental plugin (#119)
### Changed
  - Update Kotlin to `1.2.61` version
  - Update Kotlin-native dependency to `0.8.2` version
  - Update Gradle to `4.9.0` version
  - Update default KtLint version to `0.27.0`
### Fixed
  - `.editorconfig` file change doesn't reset `UP-TO-DATE` `ktlintCheck` task state (#106)


## [5.0.0] - 2018-8-6
### Added
  - Split project into multiple smaller plugins. `ktlint-base`, `ktlint-idea` & `ktlint`
### Changed
  - Update Kotlin to 1.2.50 version
  - Update Gradle to 4.8.1 version
### Removed
  - Does not automatically apply plugin tasks to all sub-projects (it breaks the Gradle plugin model).

## [4.1.0] - 2018-6-13
### Added
  - apply to IDEA task (in the project) (#91)
  - apply to IDEA task (global settings) (#91)

## [4.0.0] - 2018-5-15
### Added
 - Add support for providing custom rulesets (#71)
 - Also check `*.kts` files in Kotlin source directories
 - Use a cacheable task for the KtLint check
### Changed
  - Update Kotlin to 1.2.41 version
  - Update Gradle wrapper to 4.7 version
  - Changed default KtLint version to `0.23.1`
### Removed
  - KtLint versions prior to 0.10.0 are not supported anymore
  - Gradle versions prior to 4.3 are not supported anymore
  - Deprecated ReporterType typealias
  - Deprecated reporter field from extension

## [3.3.0] - 2018-4-24
### Added
 - Check for spaces in output path for KtLint versions earlier
then 0.20.0 (#83)
 - Use relative for input file path sensitivity (#67)
### Changed
 - Update default Ktlint version to 0.22.0

## [3.2.0] - 2018-3-19
### Changed
 - Remove usage of deprecated Gradle features (#60)
 - Update Kotlin to 1.2.30 version
 - Update Gradle to 4.6 version
 - Update default Ktlint version to 0.19.0
### Fixed
 - Fix running check task also runs some android tasks (#63)

## [3.1.0] - 2018-3-18
## Added
 - Support for Kotlin javascript (kotlin2js) (#58)
 - Support for Kotlin multiplatform projects (#58)
 - Support for Kotlin native (konan) (#58)

## [3.0.1] - 2018-2-13
### Added
 - Output to console (#38)
 - Support multiple reporters for ktlint >0.10.x (#38)
### Changed
 - Set default ktltint version to `0.15.0`
 - Update Kotlin to `1.2.21` version

## [3.0.0] - 2017-12-25
## Changed
 - Update kotlin to 1.2.0 version
 - Bumped android tools versions to 3.0 and now support multidimension projects >0.10.x (#29)

## [2.3.0] - 2017-11-13
### Added
 - Add configuration parameter `android` with default value to false (#39)
### Changed
 - Define a different output file for each sourceSet
### Fixed
 - Fixed plugin doesn't apply custom reporter for ktlint versions >0.10.x (#28)

## [2.2.1] - 2017-10-06
### Fixed
 - Fixed report output is always opened since task is created

## [2.2.0] - 2017-10-05
### Added
 - Add configuration parameter `ignoreFailures` with default value to false
### Changed
 - Update default ktlint version to 0.9.2
### Fixed
 - Fixed report output is not closed after task run is finished (#25)

## [2.1.1] - 2017-08-15
### Changed
 - Update default ktlint version to 0.8.1
 - Fix extension version has no effect on used ktlint version
 - Add check task also depends on ktlintCheck task
 - Add output report
 - Add report type to extension

## [2.1.0] - 2017-07-5
### Added
 - Android support
 - Ability to only apply plugin to the root project that contains subprojects with kotlin code

## [2.0.1] - 2017-06-1
### Changed
 - Remove dependency on Gradle Script Kotlin plugin API. (#9)
 - Compatibility with Gradle v4.0. (#9)

## [2.0.0] - 2017-05-26
### Changed
 - Renamed task with name `ktlint` to `ktlintCheck` (#3)
 - Renamed tasks with names `ktlint[source set name]` to `ktlint[source set name]Check` (#3)

[Unreleased]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v10.2.1...HEAD
[10.2.1]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v10.2.0...v10.2.1
[10.2.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v10.1.0...v10.2.0
[10.1.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v10.0.0...v10.1.0
[10.0.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v9.4.1...v10.0.0
[9.4.1]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v9.4.0...v9.4.1
[9.4.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v9.3.0...v9.4.0
[9.3.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v9.2.1...v9.3.0
[9.2.1]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v9.2.0...v9.2.1
[9.2.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v9.1.1...v9.2.0
[9.1.1]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v9.1.0...v9.1.1
[9.1.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v9.0.0...v9.1.0
[9.0.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v8.2.0...v9.0.0
[8.2.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v8.1.0...v8.2.0
[8.1.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v8.0.0...v8.1.0
[8.0.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v7.4.0...v8.0.0
