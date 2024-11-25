# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/)
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [12.1.2] - 2024-11-25

### Changed

- Bump `org.gradle.toolchains.foojay-resolver-convention` from `0.7.0` to `0.8.0` [#779](https://github.com/JLLeitschuh/ktlint-gradle/pull/779)
- Bump gradle/wrapper-validation-action from 1 to 3 [#784](https://github.com/JLLeitschuh/ktlint-gradle/pull/784)
- Bump github/codeql-action from 2.2.4 to 3.27.0 [#777](https://github.com/JLLeitschuh/ktlint-gradle/pull/777)
- Bump al-cheb/configure-pagefile-action from 1.3 to 1.4 [#780](https://github.com/JLLeitschuh/ktlint-gradle/pull/780)
- Bump commons-io:commons-io from 2.8.0 to 2.17.0 [#802](https://github.com/JLLeitschuh/ktlint-gradle/pull/802)
- Update version for jgit to 5.13.3.202401111512-r [#766](https://github.com/JLLeitschuh/ktlint-gradle/pull/766)
- builds: remove specific lintian version, as latest ubuntu is now a new version
    [#767](https://github.com/JLLeitschuh/ktlint-gradle/pull/767)    
- docs: add Version Catalog setup instructions in README
    [#770](https://github.com/JLLeitschuh/ktlint-gradle/pull/770)
    [#767](https://github.com/JLLeitschuh/ktlint-gradle/pull/767)

### Fixed

- fix tests which relied on a third party reporter in jcenter [#772](https://github.com/JLLeitschuh/ktlint-gradle/pull/772)
- Remove IntelliJ plugin references from README [#775](https://github.com/JLLeitschuh/ktlint-gradle/pull/775)

## [12.1.1] - 2024-05-07

- fix [#544](https://github.com/JLLeitschuh/ktlint-gradle/issues/544): make git filter work
  with any os [#738](https://github.com/JLLeitschuh/ktlint-gradle/pull/738)
- fix [#750](https://github.com/JLLeitschuh/ktlint-gradle/issues/750): additionalEditorconfig property not being
  accounted for up-to-date checks and caching [#758](https://github.com/JLLeitschuh/ktlint-gradle/pull/758)
- Update versions used for testing [#763](https://github.com/JLLeitschuh/ktlint-gradle/pull/763)

## [12.1.0] - 2024-01-09

- fix detection of android kotlin source directories in AGP >= 7 [#733](https://github.com/JLLeitschuh/ktlint-gradle/pull/733)

## [12.0.3] - 2023-12-11

- fix: apply configuration for source sets and targets that are added after the plugin is
  applied [#732](https://github.com/JLLeitschuh/ktlint-gradle/pull/732)

## [12.0.2] - 2023-12-01

- remove KtLintIdea Plugin [#726](https://github.com/JLLeitschuh/ktlint-gradle/pull/726).
  This plugin is no longer needed as ktlint configuration is driven by .editorconfig now, which IDEA will respect out of the box.

## [12.0.1] - 2023-11-30

- update configure-pagefile-action task [#725](https://github.com/JLLeitschuh/ktlint-gradle/pull/725)

## [12.0.0] - 2023-11-28

- update latest version text file manually [#716](https://github.com/JLLeitschuh/ktlint-gradle/pull/716)
- Fix configuration cache for relative paths [#722](https://github.com/JLLeitschuh/ktlint-gradle/pull/722)
- Drop support for Gradle 6 and ktlint &lt; 0.47.1 [#720](https://github.com/JLLeitschuh/ktlint-gradle/pull/720)

## [11.6.1] - 2023-10-10

- fix "additionalEditorconfig not supported until ktlint 0.49" warning [#712](https://github.com/JLLeitschuh/ktlint-gradle/pull/712)
- update latest version text file manually [#709](https://github.com/JLLeitschuh/ktlint-gradle/pull/709)
- Improve error logging [#711](https://github.com/JLLeitschuh/ktlint-gradle/pull/711)

## [11.6.0] - 2023-09-18

- ktlint 1.0 support [#708](https://github.com/JLLeitschuh/ktlint-gradle/pull/708)
- Allow editorconfig overrides in ktlint 0.49+ [#708](https://github.com/JLLeitschuh/ktlint-gradle/pull/708)
- update latest version text file manually [#700](https://github.com/JLLeitschuh/ktlint-gradle/pull/700)

## [11.5.1] - 2023-08-07

- Fix custom rulesets not loading from classpath [#698](https://github.com/JLLeitschuh/ktlint-gradle/pull/698)
- update latest version text file manually [#688](https://github.com/JLLeitschuh/ktlint-gradle/pull/688)

## [11.5.0] - 2023-07-03

- update latest version text file manually [#685](https://github.com/JLLeitschuh/ktlint-gradle/pull/685)
- ktlint 0.50.0 compatibility [#687](https://github.com/JLLeitschuh/ktlint-gradle/pull/687)

## [11.4.2] - 2023-06-22

- set kotlin version to 1.4 as its the minimum required for ktlint 0.49 [#683](https://github.com/JLLeitschuh/ktlint-gradle/pull/683)
- update latest version text file manually [#682](https://github.com/JLLeitschuh/ktlint-gradle/pull/682)

## [11.4.1] - 2023-06-21

### Fixed

- update latest version text file manually [#674](https://github.com/JLLeitschuh/ktlint-gradle/pull/674)
- decrease plugin build workers to 4 to prevent thrashing [#675](https://github.com/JLLeitschuh/ktlint-gradle/pull/675)
- exclude deleted files from incremental checks [#681](https://github.com/JLLeitschuh/ktlint-gradle/pull/681)

## [11.4.0] - 2023-06-06

### Changed

- Add files previously found to have errors to the list of files to check in incremental builds. [#672](https://github.com/JLLeitschuh/ktlint-gradle/pull/672)
- Added ktlint 0.49.1 support [#667](https://github.com/JLLeitschuh/ktlint-gradle/pull/667)
- Refactored multi-ktlint support to use multiple source sets with different compileOnly dependencies rather than reflection. [#667](https://github.com/JLLeitschuh/ktlint-gradle/pull/667)

## [11.3.2] - 2023-04-25

### Fixed

- fix new ktlint errors that come from our new default version of ktlint [#651](https://github.com/JLLeitschuh/ktlint-gradle/pull/651)
- fix syntax bug in release logic for VERSION_LATEST_RELEASE.txt [#651](https://github.com/JLLeitschuh/ktlint-gradle/pull/651)
- fix isRootEditorConfig [#664](https://github.com/JLLeitschuh/ktlint-gradle/pull/664)

### Changed

- Update build to use Gradle 7.3.3 to support testing with Java 17 [#658](https://github.com/JLLeitschuh/ktlint-gradle/pull/658)

## [11.3.1] - 2023-03-03

### Fixed

- Fixed release github actions [#650](https://github.com/JLLeitschuh/ktlint-gradle/pull/650)

### Changed

- warn when additionalEditorconfigFile is used in 0.47+ [#637](https://github.com/JLLeitschuh/ktlint-gradle/pull/637)
- work around reflection error on Gradle 8/JDK 16+ [#634](https://github.com/JLLeitschuh/ktlint-gradle/pull/634)
- add ktlint version 0.48.2 to testing [#632](https://github.com/JLLeitschuh/ktlint-gradle/pull/632)
- update latest gradle version for testing to 7.6 [#632](https://github.com/JLLeitschuh/ktlint-gradle/pull/632)
- improve release process to update VERSION_LATEST_RELEASE automatically [#631](https://github.com/JLLeitschuh/ktlint-gradle/pull/631)
- test against all supported jvm versions [#642](https://github.com/JLLeitschuh/ktlint-gradle/pull/642)

## [11.2.0] - 2023-02-14

### Changed

- change compile target to 0.45.2
- change default ktlint version applied by plugin to 0.47.1 [#624](https://github.com/JLLeitschuh/ktlint-gradle/pull/624)

### Fixed

- Fixed ktlint API compatibility issue around baselines in 0.46 and 0.47+
- Fixed disabled_rules warning when using new editorconfig syntax in ktlint 0.48+ [#625](https://github.com/JLLeitschuh/ktlint-gradle/pull/625)
- Fixed disabled_rules set only in editorconfig in ktlint 0.46+ [#628](https://github.com/JLLeitschuh/ktlint-gradle/pull/628)

## [11.1.0] - 2023-01-27

### Added

- The plugin will now work with ktlint `0.46.1` - `0.48.1` [#620](https://github.com/JLLeitschuh/ktlint-gradle/pull/620)

## [11.0.0] - 2022-08-24

### Changed

- **Breaking**: minimal supported Gradle version is `6.8` ([#597](https://github.com/JLLeitschuh/ktlint-gradle/pull/597))
- Update Kotlin to `1.5.31` version ([#597](https://github.com/JLLeitschuh/ktlint-gradle/pull/597))
- Set default KtLint version to `0.43.2` ([#597](https://github.com/JLLeitschuh/ktlint-gradle/pull/597))

## [10.3.0] - 2022-05-03

### Added

- `relative` option to generate reports with paths relative to the root project ([#573](https://github.com/JLLeitschuh/ktlint-gradle/pull/573))

### Fixed

- Fix install hook action when git `hooks` folder doesn't exist [issue: #557](https://github.com/JLLeitschuh/ktlint-gradle/issues/557), [#563](https://github.com/JLLeitschuh/ktlint-gradle/pull/563)
- Fix pre-commit hook command not found error [issue: #562](https://github.com/JLLeitschuh/ktlint-gradle/issues/562), [#564](https://github.com/JLLeitschuh/ktlint-gradle/pull/564)
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
  2. "kotlin-platform-\*"
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
- Make pre-commit hook working with Windows path separator [#359](https://github.com/JLLeitschuh/ktlint-gradle/pull/359)

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

- Failure on Windows due to long argument line (#156)

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

[unreleased]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v12.1.2...HEAD
[12.1.2]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v12.1.1...v12.1.2
[12.1.1]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v12.1.0...v12.1.1
[12.1.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v12.0.3...v12.1.0
[12.0.3]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v12.0.2...v12.0.3
[12.0.2]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v12.0.1...v12.0.2
[12.0.1]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v12.0.0...v12.0.1
[12.0.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v11.6.1...v12.0.0
[11.6.1]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v11.6.0...v11.6.1
[11.6.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v11.5.1...v11.6.0
[11.5.1]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v11.5.0...v11.5.1
[11.5.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v11.4.2...v11.5.0
[11.4.2]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v11.4.1...v11.4.2
[11.4.1]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v11.4.0...v11.4.1
[11.4.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v11.3.2...v11.4.0
[11.3.2]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v11.3.1...v11.3.2
[11.3.1]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v11.3.0...v11.3.1
[11.3.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v11.2.0...v11.3.0
[11.2.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v11.1.0...v11.2.0
[11.1.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v11.0.0...v11.1.0
[11.0.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v10.3.0...v11.0.0
[10.3.0]: https://github.com/JLLeitschuh/ktlint-gradle/compare/v10.2.1...v10.3.0
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
