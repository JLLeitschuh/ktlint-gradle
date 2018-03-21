# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [3.2.1]
### Changed
 - Update default Ktlint version to 0.20.0

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
